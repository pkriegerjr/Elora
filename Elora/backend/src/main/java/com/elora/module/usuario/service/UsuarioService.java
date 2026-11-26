package com.elora.module.usuario.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.common.util.DocumentUtils;
import com.elora.module.notificacao.service.AvisoSistemaService;
import com.elora.module.profissional.entity.Especialidade;
import com.elora.module.profissional.entity.UsuarioEspecialidade;
import com.elora.module.profissional.repository.EspecialidadeRepository;
import com.elora.module.profissional.repository.UsuarioEspecialidadeRepository;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.ClienteUpdateRequest;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.entity.ContratanteDetalhes;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.entity.ProfissionalDetalhes;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.entity.UsuarioPerfil;
import com.elora.module.usuario.enums.OrigemLogin;
import com.elora.module.usuario.enums.StatusVerificacao;
import com.elora.module.usuario.enums.UsuarioStatus;
import com.elora.module.usuario.repository.ContratanteDetalhesRepository;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.repository.ProfissionalDetalhesRepository;
import com.elora.module.usuario.repository.UsuarioPerfilRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Cadastro e leitura de usuários. Não altera o schema — só JPA sobre o v2.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Set<String> PERFIS_STAFF = Set.of("admin", "moderador", "juridico", "financeiro");

    private final UsuarioRepository usuarios;
    private final PerfilRepository perfis;
    private final UsuarioPerfilRepository usuarioPerfis;
    private final ProfissionalDetalhesRepository profissionalDetalhes;
    private final ContratanteDetalhesRepository contratanteDetalhes;
    private final EspecialidadeRepository especialidades;
    private final UsuarioEspecialidadeRepository usuarioEspecialidades;
    private final PasswordEncoder passwordEncoder;
    private final AvisoSistemaService avisos;

    @Transactional
    public UsuarioResponse registerCliente(ClienteRegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        String cpf = DocumentUtils.onlyDigits(req.getCpf());
        validarCadastroBase(email, cpf);

        Usuario usuario = novoUsuarioBase(req.getNome(), email, cpf, req.getTelefone(),
                req.getSenha(), req.getGenero(), req.getDataNascimento(),
                req.getLatitude(), req.getLongitude());
        usuarios.save(usuario);

        vincularPerfil(usuario, "cliente");

        ContratanteDetalhes detalhes = new ContratanteDetalhes();
        detalhes.setUsuario(usuario);
        detalhes.setObservacoesCuidado(req.getObservacoesCuidado());
        contratanteDetalhes.save(detalhes);

        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse registerCuidador(CuidadorRegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        String cpf = DocumentUtils.onlyDigits(req.getCpf());
        validarCadastroBase(email, cpf);

        Usuario usuario = novoUsuarioBase(req.getNome(), email, cpf, req.getTelefone(),
                req.getSenha(), req.getGenero(), req.getDataNascimento(),
                req.getLatitude(), req.getLongitude());
        usuarios.save(usuario);

        vincularPerfil(usuario, "profissional");

        ProfissionalDetalhes detalhes = new ProfissionalDetalhes();
        detalhes.setUsuario(usuario);
        detalhes.setDescricaoPerfil(req.getDescricaoPerfil());
        detalhes.setPrecoHora(req.getPrecoHora());
        detalhes.setDocumentoVerificado(false);
        detalhes.setStatusVerificacao(StatusVerificacao.pendente);
        detalhes.setNotaMedia(new BigDecimal("0.00"));
        profissionalDetalhes.save(detalhes);

        // Especialidades selecionadas no cadastro entram no mesmo catálogo usado pela busca.
        if (req.getEspecialidades() != null) {
            for (String nome : req.getEspecialidades()) {
                String trim = nome == null ? "" : nome.trim();
                if (trim.isEmpty()) continue;
                var esp = especialidades.findByNomeIgnoreCase(trim)
                        .orElseGet(() -> especialidades.save(new Especialidade(null, trim)));
                usuarioEspecialidades.save(new UsuarioEspecialidade(usuario.getId(), esp.getId(), null));
            }
        }

        avisos.notificarSeguro(AvisoSistemaService.ADMINISTRACAO,
                "Cuidador aguardando validação",
                usuario.getNome() + " cadastrou o perfil e aguarda validação documental.",
                "validacao", usuario.getId());

        return toResponse(usuario);
    }

    /** Atualiza o próprio perfil de contratante (campos nulos = não mexer). */
    @Transactional
    public UsuarioResponse atualizarCliente(Integer viewerId, ClienteUpdateRequest req) {
        Usuario usuario = getVisivel(viewerId);
        if (!perfisDe(viewerId).contains("cliente")) {
            throw new ForbiddenException("Este usuário não possui perfil de cliente");
        }
        if (req.getNome() != null) {
            if (req.getNome().isBlank()) {
                throw new BusinessException("Nome não pode ser vazio");
            }
            usuario.setNome(req.getNome().trim());
        }
        if (req.getTelefone() != null) {
            usuario.setTelefone(req.getTelefone().isBlank() ? null : req.getTelefone().trim());
        }
        if (req.getLatitude() != null) {
            usuario.setLatitude(BigDecimal.valueOf(req.getLatitude()));
        }
        if (req.getLongitude() != null) {
            usuario.setLongitude(BigDecimal.valueOf(req.getLongitude()));
        }
        usuarios.save(usuario);

        if (req.getObservacoesCuidado() != null) {
            ContratanteDetalhes detalhes = contratanteDetalhes.findById(viewerId)
                    .orElseGet(() -> {
                        ContratanteDetalhes novo = new ContratanteDetalhes();
                        novo.setUsuario(usuario);
                        return novo;
                    });
            detalhes.setObservacoesCuidado(req.getObservacoesCuidado());
            contratanteDetalhes.save(detalhes);
        }
        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findForViewer(Integer viewerId, Integer targetId) {
        Usuario alvo = usuarios.findById(targetId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (viewerId.equals(targetId) || perfisDe(viewerId).stream().anyMatch(PERFIS_STAFF::contains)) {
            return toResponse(alvo);
        }
        throw new ForbiddenException("Sem permissão para ver este usuário");
    }

    @Transactional(readOnly = true)
    public Usuario getVisivel(Integer id) {
        return usuarios.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<String> perfisDe(Integer usuarioId) {
        return usuarioPerfis.findByUsuario_Id(usuarioId).stream()
                .map(up -> up.getPerfil().getNome())
                .toList();
    }

    /** Envio de notificações e telas admin: só perfis internos. */
    @Transactional(readOnly = true)
    public boolean ehStaff(Integer usuarioId) {
        return perfisDe(usuarioId).stream().anyMatch(PERFIS_STAFF::contains);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse toResponse(Usuario usuario) {
        List<String> perfisDoUsuario = perfisDe(usuario.getId());
        UsuarioResponse res = new UsuarioResponse();
        res.setId(usuario.getId());
        res.setNome(usuario.getNome());
        res.setEmail(usuario.getEmail());
        res.setCpf(usuario.getCpf());
        res.setTelefone(usuario.getTelefone());
        res.setFotoUrl(usuario.getFotoUrl());
        res.setGenero(usuario.getGenero());
        res.setDataNascimento(usuario.getDataNascimento());
        res.setStatus(usuario.getStatus());
        res.setPerfis(perfisDoUsuario);
        res.setEmailVerificado(usuario.getEmailVerificado());
        res.setCriadoEm(usuario.getCriadoEm());
        if (perfisDoUsuario.contains("profissional")) {
            var prof = profissionalDetalhes.findByUsuarioId(usuario.getId());
            boolean verificado = prof.map(ProfissionalDetalhes::getDocumentoVerificado).orElse(false);
            // Fonte oficial da situação é status_verificacao (v2.2).
            String situacao = prof.map(ProfissionalDetalhes::getStatusVerificacao)
                    .map(s -> s == StatusVerificacao.aprovado ? "APPROVED" : "PENDING")
                    .orElse("PENDING");
            res.setDocumentoVerificado(verificado);
            res.setSituacaoCadastro(situacao);
            prof.ifPresent(d -> {
                res.setPrecoHora(d.getPrecoHora());
                res.setNotaMedia(d.getNotaMedia());
            });
        }
        return res;
    }

    private void validarCadastroBase(String email, String cpf) {
        if (!DocumentUtils.isValidCpf(cpf)) {
            throw new BusinessException("CPF inválido");
        }
        if (usuarios.existsByEmail(email) || usuarios.existsByCpf(cpf)) {
            throw new BusinessException("Usuário já cadastrado com este CPF ou e-mail");
        }
    }

    private Usuario novoUsuarioBase(String nome, String email, String cpf, String telefone,
                                    String senha, com.elora.module.usuario.enums.Genero genero,
                                    java.time.LocalDate dataNascimento, Double latitude, Double longitude) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(email);
        usuario.setCpf(cpf);
        usuario.setTelefone(telefone);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setOrigemLogin(OrigemLogin.senha);
        usuario.setStatus(UsuarioStatus.ativo);
        usuario.setEmailVerificado(false);
        usuario.setGenero(genero);
        usuario.setDataNascimento(dataNascimento);
        if (latitude != null && longitude != null) {
            usuario.setLatitude(BigDecimal.valueOf(latitude));
            usuario.setLongitude(BigDecimal.valueOf(longitude));
        }
        usuario.setConsentimentoLgpd(true);
        usuario.setConsentimentoLgpdAt(LocalDateTime.now());
        return usuario;
    }

    private void vincularPerfil(Usuario usuario, String nomePerfil) {
        Perfil perfil = perfis.findByNome(nomePerfil)
                .orElseThrow(() -> new BusinessException(
                        "Perfil '" + nomePerfil + "' não configurado — aplique os seeds do schema v2"));
        UsuarioPerfil vinculo = new UsuarioPerfil();
        vinculo.setUsuario(usuario);
        vinculo.setPerfil(perfil);
        usuarioPerfis.save(vinculo);
    }
}
