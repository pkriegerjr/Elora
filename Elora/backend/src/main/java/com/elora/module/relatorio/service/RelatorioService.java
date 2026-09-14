package com.elora.module.relatorio.service;

import com.elora.common.exception.ForbiddenException;
import com.elora.module.relatorio.dto.RelatorioAvaliacoesResponse;
import com.elora.module.relatorio.dto.RelatorioContratosResponse;
import com.elora.module.relatorio.dto.RelatorioDenunciasResponse;
import com.elora.module.relatorio.dto.RelatorioFinanceiroResponse;
import com.elora.module.relatorio.dto.RelatorioUsuariosResponse;
import com.elora.module.relatorio.dto.ResumoOperacionalResponse;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Relatórios somente-leitura sobre o schema v2/v2.2.
 *
 * Decisão: {@link JdbcTemplate} com SELECTs padrão + agregação em Java, em vez
 * de entities JPA. As tabelas (contrato, pagamento, avaliacao, denuncia...)
 * pertencem aos módulos futuros — mapear aqui duplicaria a posse e qualquer
 * divergência quebraria o {@code validate} no startup. Leitura não altera o banco.
 * RowMappers posicionais (índice, não nome de coluna) funcionam igual em
 * MySQL e H2.
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final Set<String> STAFF = Set.of("admin", "moderador", "juridico", "financeiro");
    private static final Set<String> FINANCEIRO = Set.of("admin", "financeiro");

    private final JdbcTemplate jdbc;
    private final UsuarioService usuarioService;

    // ------------------------------------------------------------------
    // API (com permissão)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResumoOperacionalResponse resumo(Integer viewerId) {
        exigirPerfis(viewerId, STAFF);

        List<String> usuarios = jdbc.query("SELECT status FROM usuario WHERE deleted_at IS NULL",
                (rs, i) -> rs.getString(1));
        List<String> perfis = jdbc.query(
                "SELECT pf.nome FROM usuario_perfil up " +
                "JOIN perfil pf ON pf.id_perfil = up.perfil_id " +
                "JOIN usuario u ON u.id_usuario = up.usuario_id WHERE u.deleted_at IS NULL",
                (rs, i) -> rs.getString(1));
        List<String> verifs = jdbc.query(
                "SELECT pd.status_verificacao FROM profissional_detalhes pd " +
                "JOIN usuario u ON u.id_usuario = pd.usuario_id WHERE u.deleted_at IS NULL",
                (rs, i) -> rs.getString(1));
        List<String> contratos = jdbc.query("SELECT status FROM contrato", (rs, i) -> rs.getString(1));
        List<String> pagStatuses = jdbc.query("SELECT status FROM pagamento", (rs, i) -> rs.getString(1));
        List<RepasseRow> repasses = fetchRepasses();
        List<String> denuncias = jdbc.query("SELECT status FROM denuncia", (rs, i) -> rs.getString(1));
        List<Integer> notas = fetchNotas();

        long profissionaisPendentes = verifs.stream().filter("pendente"::equals).count()
                + verifs.stream().filter("correcao"::equals).count();
        BigDecimal media = media(notas.stream().map(BigDecimal::valueOf).toList());

        return new ResumoOperacionalResponse(
                usuarios.size(),
                usuarios.stream().filter("ativo"::equals).count(),
                perfis.stream().filter("profissional"::equals).count(),
                profissionaisPendentes,
                contratos.stream().filter("ativo"::equals).count(),
                contratos.stream().filter("em_disputa"::equals).count(),
                pagStatuses.stream().filter("pendente"::equals).count(),
                repasses.stream().filter(r -> "pendente".equals(r.status())).count(),
                soma(repasses.stream().filter(r -> "pendente".equals(r.status())).map(RepasseRow::valor).toList()),
                denuncias.stream().filter(s -> "aberta".equals(s) || "em_analise".equals(s)).count(),
                media
        );
    }

    @Transactional(readOnly = true)
    public RelatorioFinanceiroResponse financeiro(Integer viewerId, LocalDate inicio, LocalDate fim) {
        exigirPerfis(viewerId, FINANCEIRO);
        return agregarFinanceiro(fetchPagamentos(inicio, fim), fetchRepasses(), inicio, fim);
    }

    @Transactional(readOnly = true)
    public RelatorioUsuariosResponse usuarios(Integer viewerId, LocalDate inicio, LocalDate fim) {
        exigirPerfis(viewerId, STAFF);
        List<String> status = jdbc.query("SELECT status FROM usuario WHERE deleted_at IS NULL",
                (rs, i) -> rs.getString(1));
        List<String> perfis = jdbc.query(
                "SELECT pf.nome FROM usuario_perfil up " +
                "JOIN perfil pf ON pf.id_perfil = up.perfil_id " +
                "JOIN usuario u ON u.id_usuario = up.usuario_id WHERE u.deleted_at IS NULL",
                (rs, i) -> rs.getString(1));
        List<String> verifs = jdbc.query(
                "SELECT pd.status_verificacao FROM profissional_detalhes pd " +
                "JOIN usuario u ON u.id_usuario = pd.usuario_id WHERE u.deleted_at IS NULL",
                (rs, i) -> rs.getString(1));
        long novos = (inicio == null || fim == null) ? status.size() : contarUsuariosNoPeriodo(inicio, fim);
        return agregarUsuarios(status, perfis, novos, verifs);
    }

    @Transactional(readOnly = true)
    public RelatorioContratosResponse contratos(Integer viewerId, LocalDate inicio, LocalDate fim) {
        exigirPerfis(viewerId, STAFF);
        List<ContratoRow> todos = fetchContratos(null, null);
        List<ContratoRow> periodo = (inicio == null || fim == null) ? todos : fetchContratos(inicio, fim);
        return agregarContratos(todos, periodo.size());
    }

    @Transactional(readOnly = true)
    public RelatorioAvaliacoesResponse avaliacoes(Integer viewerId) {
        exigirPerfis(viewerId, STAFF);
        return agregarAvaliacoes(fetchNotas());
    }

    @Transactional(readOnly = true)
    public RelatorioDenunciasResponse denuncias(Integer viewerId) {
        exigirPerfis(viewerId, STAFF);
        return agregarDenuncias(jdbc.query("SELECT status FROM denuncia", (rs, i) -> rs.getString(1)));
    }

    // ------------------------------------------------------------------
    // Fetch (SQL padrão, portée MySQL + H2)
    // ------------------------------------------------------------------

    record PagamentoRow(BigDecimal bruto, BigDecimal taxa, BigDecimal liquido, String metodo, String status) {
    }

    record RepasseRow(String status, BigDecimal valor) {
    }

    record ContratoRow(String status, BigDecimal valorHora) {
    }

    private List<PagamentoRow> fetchPagamentos(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT valor_bruto, valor_taxa, valor_liquido, metodo, status FROM pagamento";
        List<Object> args = new ArrayList<>();
        sql += periodoWhere("criado_em", inicio, fim, args);
        return jdbc.query(sql, args.toArray(), (rs, i) -> new PagamentoRow(
                rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getString(4), rs.getString(5)));
    }

    private List<RepasseRow> fetchRepasses() {
        return jdbc.query("SELECT status, valor FROM repasse",
                (rs, i) -> new RepasseRow(rs.getString(1), rs.getBigDecimal(2)));
    }

    private List<ContratoRow> fetchContratos(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT status, valor_hora FROM contrato";
        List<Object> args = new ArrayList<>();
        sql += periodoWhere("criado_em", inicio, fim, args);
        return jdbc.query(sql, args.toArray(),
                (rs, i) -> new ContratoRow(rs.getString(1), rs.getBigDecimal(2)));
    }

    private List<Integer> fetchNotas() {
        return jdbc.query("SELECT nota FROM avaliacao", (rs, i) -> rs.getInt(1));
    }

    private long contarUsuariosNoPeriodo(LocalDate inicio, LocalDate fim) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE deleted_at IS NULL AND criado_em >= ? AND criado_em < ?",
                Long.class, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
        return n == null ? 0 : n;
    }

    /** Janela semiaberta [inicio 00:00, fim+1 00:00) — sem erro de 23:59:59. */
    private static String periodoWhere(String coluna, LocalDate inicio, LocalDate fim, List<Object> args) {
        if (inicio == null || fim == null) {
            return "";
        }
        args.add(inicio.atStartOfDay());
        args.add(fim.plusDays(1).atStartOfDay());
        return " WHERE " + coluna + " >= ? AND " + coluna + " < ?";
    }

    // ------------------------------------------------------------------
    // Agregação pura (testável sem banco)
    // ------------------------------------------------------------------

    static RelatorioFinanceiroResponse agregarFinanceiro(List<PagamentoRow> pags, List<RepasseRow> reps,
                                                        LocalDate inicio, LocalDate fim) {
        Map<String, Long> porMetodo = zerar("pix", "cartao", "boleto");
        Map<String, Long> porStatus = zerar("pendente", "aprovado", "recusado", "estornado");
        pags.forEach(p -> {
            porMetodo.computeIfPresent(nvl(p.metodo()), (k, v) -> v + 1);
            porStatus.computeIfPresent(nvl(p.status()), (k, v) -> v + 1);
        });
        BigDecimal bruto = soma(pags.stream().map(PagamentoRow::bruto).toList());
        BigDecimal taxa = soma(pags.stream().map(PagamentoRow::taxa).toList());
        BigDecimal liquido = soma(pags.stream().map(PagamentoRow::liquido).toList());
        return new RelatorioFinanceiroResponse(
                inicio == null ? null : inicio.toString(),
                fim == null ? null : fim.toString(),
                pags.size(), bruto, taxa, liquido, media(pags.stream().map(PagamentoRow::liquido).toList()),
                porMetodo, porStatus,
                reps.stream().filter(r -> "pendente".equals(r.status())).count(),
                reps.stream().filter(r -> "processado".equals(r.status())).count(),
                soma(reps.stream().filter(r -> "pendente".equals(r.status())).map(RepasseRow::valor).toList())
        );
    }

    static RelatorioUsuariosResponse agregarUsuarios(List<String> status, List<String> perfis,
                                                    long novos, List<String> verifs) {
        Map<String, Long> porStatus = zerar("ativo", "inativo", "suspenso");
        status.forEach(s -> porStatus.computeIfPresent(nvl(s), (k, v) -> v + 1));
        Map<String, Long> porPerfil = new LinkedHashMap<>();
        perfis.forEach(p -> porPerfil.merge(nvl(p), 1L, Long::sum));
        Map<String, Long> porVerif = zerar("pendente", "em_analise", "aprovado", "rejeitado", "correcao");
        verifs.forEach(v -> porVerif.computeIfPresent(nvl(v), (k, c) -> c + 1));
        return new RelatorioUsuariosResponse(
                status.size(),
                status.stream().filter("ativo"::equals).count(),
                novos, porPerfil, porStatus, porVerif
        );
    }

    static RelatorioContratosResponse agregarContratos(List<ContratoRow> todos, long novos) {
        Map<String, Long> porStatus = zerar("rascunho", "proposta", "negociacao", "aguard_assinatura",
                "ativo", "concluido", "rescindido", "cancelado", "em_disputa");
        todos.forEach(c -> porStatus.computeIfPresent(nvl(c.status()), (k, v) -> v + 1));
        List<BigDecimal> valores = todos.stream().map(ContratoRow::valorHora).filter(v -> v != null).toList();
        return new RelatorioContratosResponse(todos.size(), novos, media(valores), porStatus);
    }

    static RelatorioAvaliacoesResponse agregarAvaliacoes(List<Integer> notas) {
        Map<String, Long> dist = zerar("1", "2", "3", "4", "5");
        notas.forEach(n -> dist.computeIfPresent(String.valueOf(n), (k, v) -> v + 1));
        return new RelatorioAvaliacoesResponse(
                notas.size(), media(notas.stream().map(BigDecimal::valueOf).toList()), dist);
    }

    static RelatorioDenunciasResponse agregarDenuncias(List<String> status) {
        Map<String, Long> porStatus = zerar("aberta", "em_analise", "resolvida", "arquivada");
        status.forEach(s -> porStatus.computeIfPresent(nvl(s), (k, v) -> v + 1));
        long abertas = status.stream().filter(s -> "aberta".equals(s) || "em_analise".equals(s)).count();
        return new RelatorioDenunciasResponse(status.size(), abertas, porStatus);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private void exigirPerfis(Integer viewerId, Set<String> permitidos) {
        boolean ok = usuarioService.perfisDe(viewerId).stream().anyMatch(permitidos::contains);
        if (!ok) {
            throw new ForbiddenException("Relatórios restritos à equipe Elora");
        }
    }

    private static Map<String, Long> zerar(String... chaves) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String k : chaves) {
            m.put(k, 0L);
        }
        return m;
    }

    private static String nvl(String v) {
        return v == null ? "?" : v;
    }

    private static BigDecimal soma(List<BigDecimal> valores) {
        return valores.stream()
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal media(List<BigDecimal> valores) {
        List<BigDecimal> validos = valores.stream().filter(v -> v != null).toList();
        if (validos.isEmpty()) {
            return new BigDecimal("0.00");
        }
        return soma(validos).divide(BigDecimal.valueOf(validos.size()), 2, RoundingMode.HALF_UP);
    }
}
