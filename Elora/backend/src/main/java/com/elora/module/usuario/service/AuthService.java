package com.elora.module.usuario.service;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.UnauthorizedException;
import com.elora.common.util.DocumentUtils;
import com.elora.module.usuario.dto.AuthResponse;
import com.elora.module.usuario.dto.LoginRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.entity.RedefinicaoSenha;
import com.elora.module.usuario.entity.Sessao;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.enums.UsuarioStatus;
import com.elora.module.usuario.repository.RedefinicaoSenhaRepository;
import com.elora.module.usuario.repository.SessaoRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Login por e-mail ou CPF + refresh token opaco com rotação.
 * Refresh: só o hash SHA-256 é persistido em {@code sessao} (v2, REQ-017).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "CPF/E-mail ou senha incorretos";
    private static final String SESSAO_EXPIRADA = "Sessão expirada. Entre novamente.";

    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final RedefinicaoSenhaRepository redefinicoes;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    @Value("${app.jwt.refresh-expiration:2592000000}")
    private long refreshExpirationMs;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AuthResponse login(LoginRequest req, String ip, String userAgent) {
        Usuario usuario = localizarPorIdentifier(req.getIdentifier())
                .orElseThrow(() -> new UnauthorizedException(CREDENCIAIS_INVALIDAS));

        if (usuario.getSenhaHash() == null || !passwordEncoder.matches(req.getPassword(), usuario.getSenhaHash())) {
            throw new UnauthorizedException(CREDENCIAIS_INVALIDAS);
        }
        if (usuario.getStatus() != UsuarioStatus.ativo) {
            throw new BusinessException("Conta " + usuario.getStatus().name() + ". Fale com o suporte.");
        }
        usuario.setUltimoLoginEm(LocalDateTime.now());

        List<String> perfis = usuarioService.perfisDe(usuario.getId());
        String accessToken = gerarAccessToken(usuario, perfis);
        String refreshToken = novaSessao(usuario, ip, userAgent);

        UsuarioResponse user = usuarioService.toResponse(usuario);
        return new AuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String ip, String userAgent) {
        Sessao sessao = sessoes.findByRefreshHashAndRevogadaFalse(sha256Hex(refreshToken))
                .orElseThrow(() -> new UnauthorizedException(SESSAO_EXPIRADA));

        if (sessao.getExpiraEm().isBefore(LocalDateTime.now())) {
            sessao.setRevogada(true);
            sessoes.save(sessao);
            throw new UnauthorizedException(SESSAO_EXPIRADA);
        }

        Usuario usuario = usuarioService.getVisivel(sessao.getUsuario().getId());
        if (usuario.getStatus() != UsuarioStatus.ativo) {
            throw new BusinessException("Conta " + usuario.getStatus().name() + ". Fale com o suporte.");
        }

        // Rotação: invalida o refresh usado e emite par novo.
        sessao.setRevogada(true);
        sessoes.save(sessao);

        List<String> perfis = usuarioService.perfisDe(usuario.getId());
        String accessToken = gerarAccessToken(usuario, perfis);
        String novoRefresh = novaSessao(usuario, ip, userAgent);

        return new AuthResponse(accessToken, novoRefresh, usuarioService.toResponse(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(Integer usuarioId) {
        return usuarioService.toResponse(usuarioService.getVisivel(usuarioId));
    }

    /**
     * REQ-ELO-002 sobre {@code redefinicao_senha} (v2.2). Sempre retorna o
     * token opaco quando o usuário existe; quando não existe, não faz nada
     * (resposta genérica no controller — não revela cadastros). Envio do
     * token por e-mail/SMS é integração futura (mesmo padrão do disparo
     * de notificações); em dev o token volta no log.
     *
     * @return token opaco (uso único, expira em 1h) ou null se não há usuário
     */
    @Transactional
    public String requestPasswordReset(String identifier) {
        var usuario = localizarPorIdentifier(identifier);
        if (usuario.isEmpty()) {
            return null;
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RedefinicaoSenha reset = new RedefinicaoSenha();
        reset.setUsuario(usuario.get());
        reset.setTokenHash(sha256Hex(raw));
        reset.setExpiraEm(LocalDateTime.now().plusHours(1));
        redefinicoes.save(reset);
        return raw;
    }

    /**
     * Consome o token de {@link #requestPasswordReset(String)}: marca uso,
     * troca a senha e derruba todas as sessões (força relogin).
     */
    @Transactional
    public void confirmPasswordReset(String token, String novaSenha) {
        RedefinicaoSenha reset = redefinicoes.findByTokenHash(sha256Hex(token == null ? "" : token))
                .orElseThrow(() -> new UnauthorizedException("Token inválido ou expirado"));
        if (reset.getUsadoEm() != null || reset.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Token inválido ou expirado");
        }
        Usuario usuario = usuarioService.getVisivel(reset.getUsuario().getId());
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarios.save(usuario);

        reset.setUsadoEm(LocalDateTime.now());
        redefinicoes.save(reset);
        sessoes.deleteByUsuario_Id(usuario.getId());
    }

    private java.util.Optional<Usuario> localizarPorIdentifier(String identifier) {
        String id = identifier == null ? "" : identifier.trim();
        if (id.contains("@")) {
            return usuarios.findByEmailAndDeletedAtIsNull(id.toLowerCase());
        }
        String cpf = DocumentUtils.onlyDigits(id);
        if (cpf.length() != 11) {
            return java.util.Optional.empty();
        }
        return usuarios.findByCpfAndDeletedAtIsNull(cpf);
    }

    private String gerarAccessToken(Usuario usuario, List<String> perfis) {
        return jwt.generate(usuario.getId().toString(), Map.of(
                "email", usuario.getEmail(),
                "nome", usuario.getNome(),
                "perfis", perfis
        ));
    }

    private String novaSessao(Usuario usuario, String ip, String userAgent) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Sessao sessao = new Sessao();
        sessao.setUsuario(usuario);
        sessao.setRefreshHash(sha256Hex(raw));
        sessao.setIp(ip);
        sessao.setUserAgent(userAgent);
        sessao.setExpiraEm(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        sessao.setRevogada(false);
        sessoes.save(sessao);
        return raw;
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
