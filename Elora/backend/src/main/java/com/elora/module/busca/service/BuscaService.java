package com.elora.module.busca.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.busca.dto.EspecialidadeResponse;
import com.elora.module.busca.dto.FavoritoRequest;
import com.elora.module.busca.dto.FavoritoResponse;
import com.elora.module.busca.dto.ProfissionalResponse;
import com.elora.module.busca.entity.Disponibilidade;
import com.elora.module.busca.entity.Especialidade;
import com.elora.module.busca.entity.Favorito;
import com.elora.module.busca.entity.UsuarioEspecialidade;
import com.elora.module.busca.enums.OrdenacaoBusca;
import com.elora.module.busca.enums.Periodo;
import com.elora.module.busca.enums.StatusDisponibilidade;
import com.elora.module.busca.mapper.BuscaMapper;
import com.elora.module.busca.repository.DisponibilidadeRepository;
import com.elora.module.busca.repository.EspecialidadeRepository;
import com.elora.module.busca.repository.FavoritoRepository;
import com.elora.module.busca.repository.UsuarioEspecialidadeRepository;
import com.elora.module.usuario.entity.ProfissionalDetalhes;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.repository.ProfissionalDetalhesRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.module.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MÓDULO BUSCA - Regras de negócio (JPA para acesso + filtros em Java).
 *
 * <p>Reutiliza o módulo usuario SEM modificá-lo (UsuarioRepository,
 * ProfissionalDetalhesRepository e UsuarioService — igual ao NotificacaoService).
 * Filtros em Java de propósito: combinações opcionais em JPQL viram query
 * gigante; em Java cada filtro é um método pequeno e testável. Se a base
 * crescer muito, a evolução é trocar por Specification (registrado, não feito).</p>
 *
 * <p>Quantitativos (ordenam): preco/nota/nome + direção ASC/DESC (extensível
 * via {@link OrdenacaoBusca}). Qualitativos (filtram): especialidade,
 * agenda, nome, faixa de preço, nota mínima.</p>
 */
@Service
@RequiredArgsConstructor
public class BuscaService {

    // Módulo usuario (reutilizado, sem modificação).
    private final UsuarioRepository usuarios;
    private final ProfissionalDetalhesRepository profissionalDetalhes;
    private final UsuarioService usuarioService;

    // Tabelas próprias da busca.
    private final EspecialidadeRepository especialidades;
    private final UsuarioEspecialidadeRepository usuarioEspecialidades;
    private final DisponibilidadeRepository disponibilidades;
    private final FavoritoRepository favoritos;

    private static final String PERFIL_PROFISSIONAL = "profissional";

    /**
     * Busca combinada: todos os parâmetros opcionais; sem nenhum, lista tudo.
     * precoMin > precoMax = 422 (filtro incoerente).
     */
    @Transactional(readOnly = true)
    public List<ProfissionalResponse> buscar(String nome, BigDecimal precoMin, BigDecimal precoMax,
                                             BigDecimal notaMinima, Integer especialidadeId,
                                             LocalDate data, Periodo periodo,
                                             String ordenarPor, String direcao, int limit) {
        if (precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0) {
            throw new BusinessException("precoMin não pode ser maior que precoMax");
        }
        OrdenacaoBusca ordem = OrdenacaoBusca.parse(ordenarPor);
        boolean asc = parseDirecao(direcao);

        Set<Integer> comEspecialidade = idsComEspecialidade(especialidadeId);
        Set<Integer> livresNaAgenda = idsLivresNaAgenda(data, periodo);
        Map<Integer, String> nomesEsp = mapaEspecialidades();

        List<ProfissionalResponse> resultado = profissionalDetalhes.findAll().stream()
                .map(d -> usuarios.findById(d.getUsuarioId()).orElse(null))
                .filter(u -> u != null && u.getDeletedAt() == null)
                .filter(u -> usuarioService.perfisDe(u.getId()).contains(PERFIL_PROFISSIONAL))
                .filter(u -> passaNoNome(u, nome))
                .filter(u -> passaNoPreco(u, precoMin, precoMax))
                .filter(u -> passaNaNota(u, notaMinima))
                .filter(u -> comEspecialidade == null || comEspecialidade.contains(u.getId()))
                .filter(u -> livresNaAgenda == null || livresNaAgenda.contains(u.getId()))
                .map(u -> BuscaMapper.toProfissionalResponse(
                        u, profissionalDetalhes.findByUsuarioId(u.getId()).orElse(null),
                        nomesEspecialidadesDe(u.getId(), nomesEsp)))
                .collect(Collectors.toList());

        ordenar(resultado, ordem, asc);
        return resultado.stream().limit(limit).toList();
    }

    /** Perfil público do cuidador (404 se não existe/não é profissional). */
    @Transactional(readOnly = true)
    public ProfissionalResponse detalhar(Integer profissionalId) {
        Usuario u = usuarioService.getVisivel(profissionalId);
        if (!usuarioService.perfisDe(profissionalId).contains(PERFIL_PROFISSIONAL)) {
            throw new ResourceNotFoundException("Profissional não encontrado");
        }
        return BuscaMapper.toProfissionalResponse(
                u, profissionalDetalhes.findByUsuarioId(profissionalId).orElse(null),
                nomesEspecialidadesDe(profissionalId, mapaEspecialidades()));
    }

    /** Catálogo de tags (A–Z, vem ordenado do repository). */
    @Transactional(readOnly = true)
    public List<EspecialidadeResponse> listarEspecialidades() {
        return especialidades.findAllByOrderByNomeAsc().stream()
                .map(e -> new EspecialidadeResponse(e.getId(), e.getNome()))
                .toList();
    }

    /** Favorita (coração). Quem favorita é o logado. */
    @Transactional
    public FavoritoResponse favoritar(Integer viewerId, FavoritoRequest req) {
        if (req.getProfissionalId() == null) {
            throw new BusinessException("profissionalId é obrigatório");
        }
        usuarioService.getVisivel(req.getProfissionalId()); // 404
        if (!usuarioService.perfisDe(req.getProfissionalId()).contains(PERFIL_PROFISSIONAL)) {
            throw new BusinessException("Só é possível favoritar profissionais");
        }
        if (viewerId.equals(req.getProfissionalId())) {
            throw new BusinessException("Você não pode favoritar a si mesmo");
        }
        if (favoritos.existsByClienteIdAndProfissionalId(viewerId, req.getProfissionalId())) {
            throw new BusinessException("Este profissional já está nos seus favoritos");
        }
        Favorito salvo = favoritos.save(new Favorito(viewerId, req.getProfissionalId(), null));
        return BuscaMapper.toFavoritoResponse(salvo);
    }

    /** Favoritos do logado (cartões). Pula quem foi apagado depois. */
    @Transactional(readOnly = true)
    public List<ProfissionalResponse> meusFavoritos(Integer viewerId) {
        Map<Integer, String> nomesEsp = mapaEspecialidades();
        return favoritos.findByClienteIdOrderByCriadoEmDesc(viewerId).stream()
                .map(f -> usuarios.findById(f.getProfissionalId()).orElse(null))
                .filter(u -> u != null && u.getDeletedAt() == null)
                .map(u -> BuscaMapper.toProfissionalResponse(
                        u, profissionalDetalhes.findByUsuarioId(u.getId()).orElse(null),
                        nomesEspecialidadesDe(u.getId(), nomesEsp)))
                .toList();
    }

    /** Desfavorita (idempotente: se não existia, nada acontece). */
    @Transactional
    public void desfavoritar(Integer viewerId, Integer profissionalId) {
        favoritos.deleteByClienteIdAndProfissionalId(viewerId, profissionalId);
    }

    // ------------------------------------------------------------------
    // Apoio (um método por filtro — fácil de ler e testar)
    // ------------------------------------------------------------------

    private Set<Integer> idsComEspecialidade(Integer especialidadeId) {
        if (especialidadeId == null) {
            return null;
        }
        especialidades.findById(especialidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada"));
        return usuarioEspecialidades.findByEspecialidadeId(especialidadeId).stream()
                .map(UsuarioEspecialidade::getUsuarioId)
                .collect(Collectors.toSet());
    }

    private Set<Integer> idsLivresNaAgenda(LocalDate data, Periodo periodo) {
        if (data == null || periodo == null) {
            return null; // filtro de agenda exige os dois juntos
        }
        return disponibilidades
                .findByDataAndPeriodoAndStatus(data, periodo, StatusDisponibilidade.disponivel)
                .stream().map(d -> d.getUsuario().getId())
                .collect(Collectors.toSet());
    }

    private boolean passaNoNome(Usuario u, String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return true;
        }
        return u.getNome() != null && u.getNome().toLowerCase().contains(nome.trim().toLowerCase());
    }

    private boolean passaNoPreco(Usuario u, BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return true;
        }
        BigDecimal preco = profissionalDetalhes.findByUsuarioId(u.getId())
                .map(ProfissionalDetalhes::getPrecoHora).orElse(null);
        if (preco == null) {
            return false;
        }
        return (min == null || preco.compareTo(min) >= 0)
                && (max == null || preco.compareTo(max) <= 0);
    }

    private boolean passaNaNota(Usuario u, BigDecimal notaMinima) {
        if (notaMinima == null) {
            return true;
        }
        BigDecimal nota = profissionalDetalhes.findByUsuarioId(u.getId())
                .map(ProfissionalDetalhes::getNotaMedia).orElse(BigDecimal.ZERO);
        return nota.compareTo(notaMinima) >= 0;
    }

    private Map<Integer, String> mapaEspecialidades() {
        return especialidades.findAll().stream()
                .collect(Collectors.toMap(Especialidade::getId, Especialidade::getNome));
    }

    private List<String> nomesEspecialidadesDe(Integer usuarioId, Map<Integer, String> nomes) {
        return usuarioEspecialidades.findByUsuarioId(usuarioId).stream()
                .map(v -> nomes.getOrDefault(v.getEspecialidadeId(), "?"))
                .toList();
    }

    /** Direção: ASC/asc = true, DESC/desc = false; outra coisa = 422. */
    private boolean parseDirecao(String direcao) {
        if (direcao == null || direcao.equalsIgnoreCase("DESC")) {
            return false;
        }
        if (direcao.equalsIgnoreCase("ASC")) {
            return true;
        }
        throw new BusinessException("direcao inválida (use ASC ou DESC)");
    }

    /** Ordenação por critério + direção (extensível via OrdenacaoBusca). */
    private void ordenar(List<ProfissionalResponse> lista, OrdenacaoBusca ordem, boolean asc) {
        Comparator<ProfissionalResponse> cmp;
        switch (ordem) {
            case preco:
                cmp = Comparator.comparing(ProfissionalResponse::getPrecoHora,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case nome:
                cmp = Comparator.comparing(
                        p -> p.getNome() == null ? "" : p.getNome().toLowerCase());
                break;
            case nota:
            default:
                cmp = Comparator.comparing(ProfissionalResponse::getNotaMedia,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }
        lista.sort(asc ? cmp : cmp.reversed());
    }
}
