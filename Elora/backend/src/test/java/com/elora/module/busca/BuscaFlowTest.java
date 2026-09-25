package com.elora.module.busca;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.busca.dto.FavoritoRequest;
import com.elora.module.busca.dto.ProfissionalResponse;
import com.elora.module.busca.entity.Disponibilidade;
import com.elora.module.busca.entity.Especialidade;
import com.elora.module.busca.entity.UsuarioEspecialidade;
import com.elora.module.busca.enums.Periodo;
import com.elora.module.busca.enums.StatusDisponibilidade;
import com.elora.module.busca.repository.DisponibilidadeRepository;
import com.elora.module.busca.repository.EspecialidadeRepository;
import com.elora.module.busca.repository.UsuarioEspecialidadeRepository;
import com.elora.module.busca.service.BuscaService;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.repository.ProfissionalDetalhesRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.module.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2 (padrão NotificacaoFlowTest): sem filtros, tag
 * qualitativa, faixa de valor, ASC/DESC, combinação, filtros inválidos,
 * detalhe, favoritos e catálogo. Rollback ao final.
 */
@SpringBootTest
@Transactional
class BuscaFlowTest {

    @Autowired
    private BuscaService buscaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfissionalDetalhesRepository detalhesRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private EspecialidadeRepository especialidadeRepository;

    @Autowired
    private UsuarioEspecialidadeRepository vinculosRepository;

    @Autowired
    private DisponibilidadeRepository disponibilidadeRepository;

    private Integer cli;
    private Integer p1;
    private Integer p2;
    private Integer p3;
    private Integer teaId;
    private LocalDate amanha;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");
        cli = cliente("cli@t.com", "123456789");
        // preços diferentes p/ ordenar; notas diferentes p/ filtrar e ordenar
        p1 = cuidador("p1@t.com", "987654321", new BigDecimal("50.00"), new BigDecimal("4.80"));
        p2 = cuidador("p2@t.com", "135792468", new BigDecimal("30.00"), new BigDecimal("3.50"));
        p3 = cuidador("p3@t.com", "246813579", new BigDecimal("40.00"), new BigDecimal("5.00"));

        Especialidade tea = new Especialidade();
        tea.setNome("TEA");
        teaId = especialidadeRepository.save(tea).getId();
        vinculosRepository.save(new UsuarioEspecialidade(p1, teaId));

        amanha = LocalDate.now().plusDays(1);
        Disponibilidade agenda = new Disponibilidade();
        agenda.setUsuario(usuarioRepository.findById(p1).orElseThrow());
        agenda.setData(amanha);
        agenda.setPeriodo(Periodo.matutino);
        agenda.setStatus(StatusDisponibilidade.disponivel);
        disponibilidadeRepository.save(agenda);
    }

    @Test
    void semFiltros() {
        List<ProfissionalResponse> r =
                buscaService.buscar(null, null, null, null, null, null, null, "nota", "DESC", 20);
        assertEquals(3, r.size());
        // padrão: nota DESC → p3 (5.0), p1 (4.8), p2 (3.5)
        assertEquals(p3, r.get(0).getId());
        assertEquals(p1, r.get(1).getId());
        assertEquals(p2, r.get(2).getId());
    }

    @Test
    void filtroQualitativo() {
        List<ProfissionalResponse> r =
                buscaService.buscar(null, null, null, null, teaId, null, null, "nota", "DESC", 20);
        assertEquals(1, r.size());
        assertEquals(p1, r.get(0).getId());
        assertTrue(r.get(0).getEspecialidades().contains("TEA"));
    }

    @Test
    void especialidadeInexistente() {
        assertThrows(ResourceNotFoundException.class, () ->
                buscaService.buscar(null, null, null, null, 99999, null, null, "nota", "DESC", 20));
    }

    @Test
    void filtroValorMaximo() {
        List<ProfissionalResponse> r =
                buscaService.buscar(null, null, new BigDecimal("30"), null,
                        null, null, null, "nota", "DESC", 20);
        assertEquals(1, r.size());
        assertEquals(p2, r.get(0).getId());
    }

    @Test
    void ordenacaoAscDesc() {
        List<ProfissionalResponse> asc =
                buscaService.buscar(null, null, null, null, null, null, null, "preco", "ASC", 20);
        assertEquals(p2, asc.get(0).getId()); // 30 o mais barato
        assertEquals(p1, asc.get(2).getId()); // 50 o mais caro
        List<ProfissionalResponse> desc =
                buscaService.buscar(null, null, null, null, null, null, null, "preco", "DESC", 20);
        assertEquals(p1, desc.get(0).getId());
    }

    @Test
    void buscaCombinada() {
        // TEA + teto 60 + menor preço primeiro → só p1
        List<ProfissionalResponse> r = buscaService.buscar(null, null, new BigDecimal("60"),
                null, teaId, null, null, "preco", "ASC", 20);
        assertEquals(1, r.size());
        assertEquals(p1, r.get(0).getId());
        // TEA + teto 30 → vazio (AND de verdade)
        List<ProfissionalResponse> vazio = buscaService.buscar(null, null, new BigDecimal("30"),
                null, teaId, null, null, "preco", "ASC", 20);
        assertTrue(vazio.isEmpty());
    }

    @Test
    void filtroAgenda() {
        List<ProfissionalResponse> r = buscaService.buscar(null, null, null, null, null,
                amanha, Periodo.matutino, "nota", "DESC", 20);
        assertEquals(1, r.size());
        assertEquals(p1, r.get(0).getId());
    }

    @Test
    void filtrosInvalidos() {
        assertThrows(BusinessException.class, () ->
                buscaService.buscar(null, new BigDecimal("50"), new BigDecimal("30"),
                        null, null, null, null, "nota", "DESC", 20));
        assertThrows(BusinessException.class, () ->
                buscaService.buscar(null, null, null, null, null, null, null, "cor", "DESC", 20));
        assertThrows(BusinessException.class, () ->
                buscaService.buscar(null, null, null, null, null, null, null, "nota", "LADO", 20));
    }

    @Test
    void detalhar() {
        ProfissionalResponse p = buscaService.detalhar(p1);
        assertTrue(p.getNome().contains("p1@t.com"));
        assertTrue(p.getEspecialidades().contains("TEA"));
        assertThrows(ResourceNotFoundException.class, () -> buscaService.detalhar(cli));
    }

    @Test
    void favoritos() {
        var fav = buscaService.favoritar(cli, new FavoritoRequest(p1));
        assertEquals(cli, fav.getClienteId());
        assertThrows(BusinessException.class,
                () -> buscaService.favoritar(cli, new FavoritoRequest(p1))); // duplicado
        assertThrows(BusinessException.class,
                () -> buscaService.favoritar(p1, new FavoritoRequest(p1))); // self
        assertThrows(BusinessException.class,
                () -> buscaService.favoritar(p1, new FavoritoRequest(cli))); // cliente não é prof
        assertEquals(1, buscaService.meusFavoritos(cli).size());
        buscaService.desfavoritar(cli, p1);
        assertTrue(buscaService.meusFavoritos(cli).isEmpty());
    }

    @Test
    void catalogo() {
        assertTrue(buscaService.listarEspecialidades().stream()
                .anyMatch(e -> "TEA".equals(e.getNome())));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private void perfil(String nome) {
        if (perfilRepository.findByNome(nome).isEmpty()) {
            Perfil p = new Perfil();
            p.setNome(nome);
            p.setDescricao("seed de teste");
            perfilRepository.save(p);
        }
    }

    private Integer cliente(String email, String base9) {
        ClienteRegisterRequest req = new ClienteRegisterRequest();
        req.setNome("Cliente " + email);
        req.setEmail(email);
        req.setCpf(cpf(base9));
        req.setSenha("Senha@123");
        return usuarioService.registerCliente(req).getId();
    }

    private Integer cuidador(String email, String base9, BigDecimal preco, BigDecimal nota) {
        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
        req.setNome("Cuidador " + email);
        req.setEmail(email);
        req.setCpf(cpf(base9));
        req.setSenha("Senha@123");
        req.setPrecoHora(preco);
        Integer id = usuarioService.registerCuidador(req).getId();
        var detalhes = detalhesRepository.findByUsuarioId(id).orElseThrow();
        detalhes.setNotaMedia(nota);
        detalhesRepository.save(detalhes);
        return id;
    }

    static String cpf(String base9) {
        int d1 = dv(base9, 10);
        int d2 = dv(base9 + d1, 11);
        return base9 + d1 + d2;
    }

    private static int dv(String s, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < s.length(); i++) {
            soma += (s.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
