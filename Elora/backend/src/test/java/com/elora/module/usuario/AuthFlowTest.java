package com.elora.module.usuario;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.UnauthorizedException;
import com.elora.module.profissional.repository.UsuarioEspecialidadeRepository;
import com.elora.module.usuario.dto.AuthResponse;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.dto.LoginRequest;
import com.elora.module.usuario.dto.RefreshRequest;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.entity.UsuarioPerfil;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.repository.SessaoRepository;
import com.elora.module.usuario.repository.UsuarioPerfilRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.module.usuario.service.AuthService;
import com.elora.module.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2 (create-drop): cadastro → login → me → refresh.
 * Rollback ao final — não suja nada.
 */
@SpringBootTest
@Transactional
class AuthFlowTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private SessaoRepository sessoes;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private UsuarioPerfilRepository usuarioPerfis;

    @Autowired
    private UsuarioEspecialidadeRepository usuarioEspecialidades;

    @BeforeEach
    void seeds() {
        for (String nome : new String[]{"cliente", "profissional", "admin"}) {
            if (perfilRepository.findByNome(nome).isEmpty()) {
                Perfil p = new Perfil();
                p.setNome(nome);
                p.setDescricao("seed de teste");
                perfilRepository.save(p);
            }
        }
    }

    private ClienteRegisterRequest cadastro(String email, String cpf) {
        ClienteRegisterRequest req = new ClienteRegisterRequest();
        req.setNome("Maria Santos");
        req.setEmail(email);
        req.setCpf(cpf);
        req.setTelefone("(13) 99999-0000");
        req.setSenha("Senha@123");
        req.setConsentimentoLgpd(true);
        return req;
    }

    private Integer criarAdmin(String email, String cpf) {
        Integer id = usuarioService.registerCliente(cadastro(email, cpf)).getId();
        Usuario usuario = usuarios.findById(id).orElseThrow();
        UsuarioPerfil vinculo = new UsuarioPerfil();
        vinculo.setUsuario(usuario);
        vinculo.setPerfil(perfilRepository.findByNome("admin").orElseThrow());
        usuarioPerfis.save(vinculo);
        return id;
    }

    @Test
    void cadastroLoginMeRefresh() {
        var criado = usuarioService.registerCliente(cadastro("maria@teste.com", "52998224725"));
        assertNotNull(criado.getId());
        assertEquals(List.of("cliente"), criado.getPerfis());

        AuthResponse login = authService.login(
                new LoginRequest("maria@teste.com", "Senha@123", "CLIENT"), "127.0.0.1", "teste");
        assertNotNull(login.getAccessToken());
        assertNotNull(login.getRefreshToken());
        assertEquals(criado.getId(), login.getUser().getId());

        // login por CPF com máscara também funciona
        AuthResponse porCpf = authService.login(
                new LoginRequest("529.982.247-25", "Senha@123", "CLIENT"), "127.0.0.1", "teste");
        assertEquals(criado.getId(), porCpf.getUser().getId());

        var me = authService.me(criado.getId());
        assertEquals("maria@teste.com", me.getEmail());

        AuthResponse renovado = authService.refresh(
                new RefreshRequest(login.getRefreshToken()).getRefreshToken(), "127.0.0.1", "teste");
        assertNotNull(renovado.getAccessToken());

        // refresh antigo foi rotacionado — reutilizar deve falhar
        assertThrows(UnauthorizedException.class, () ->
                authService.refresh(login.getRefreshToken(), "127.0.0.1", "teste"));
    }

    @Test
    void loginSenhaErradaDa401() {
        usuarioService.registerCliente(cadastro("joao@teste.com", "11144477735"));
        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("joao@teste.com", "Errada@123", "CLIENT"), null, null));
    }

    @Test
    void cadastroDuplicadoDa422() {
        usuarioService.registerCliente(cadastro("dup@teste.com", "52998224725"));
        assertThrows(BusinessException.class, () ->
                usuarioService.registerCliente(cadastro("dup@teste.com", "11144477735")));
    }

    @Test
    void cpfInvalidoDa422() {
        assertThrows(BusinessException.class, () ->
                usuarioService.registerCliente(cadastro("x@teste.com", "11111111111")));
    }

    @Test
    void cadastroCuidadorComecaPendente() {        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
        req.setNome("Ana Ferreira");
        req.setEmail("ana@teste.com");
        req.setCpf("11144477735");
        req.setSenha("Senha@123");
        req.setConsentimentoLgpd(true);
        req.setPrecoHora(new java.math.BigDecimal("45.00"));

        var criado = usuarioService.registerCuidador(req);
        assertNotNull(criado.getId());
        assertEquals(List.of("profissional"), criado.getPerfis());
        assertEquals("PENDING", criado.getSituacaoCadastro());

        // login registra ultimo_login_em (coluna v2.2)
        authService.login(new LoginRequest("ana@teste.com", "Senha@123", "CAREGIVER"), null, null);
        assertNotNull(authService.me(criado.getId()).getCriadoEm());
    }

    @Test
    void loginComAbaDiferenteDoPerfilFalhaSemCriarSessao() {
        Integer clienteId = usuarioService.registerCliente(cadastro("aba-cliente@teste.com", "52998224725")).getId();

        CuidadorRegisterRequest cg = new CuidadorRegisterRequest();
        cg.setNome("Ana Aba");
        cg.setEmail("aba-cuidador@teste.com");
        cg.setCpf("11144477735");
        cg.setSenha("Senha@123");
        cg.setConsentimentoLgpd(true);
        cg.setPrecoHora(new java.math.BigDecimal("45.00"));
        Integer cuidadorId = usuarioService.registerCuidador(cg).getId();

        long sessoesAntes = sessoes.count();

        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("aba-cliente@teste.com", "Senha@123", "CAREGIVER"), "127.0.0.1", "teste"));
        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("aba-cliente@teste.com", "Senha@123", "ADMIN"), "127.0.0.1", "teste"));
        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("aba-cuidador@teste.com", "Senha@123", "CLIENT"), "127.0.0.1", "teste"));
        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("aba-cuidador@teste.com", "Senha@123", "ADMIN"), "127.0.0.1", "teste"));

        assertEquals(sessoesAntes, sessoes.count(), "tentativas rejeitadas não podem criar sessão");
        assertNull(usuarios.findById(clienteId).orElseThrow().getUltimoLoginEm());
        assertNull(usuarios.findById(cuidadorId).orElseThrow().getUltimoLoginEm());

        // mesmas credenciais na aba correta continuam funcionando
        assertEquals(clienteId, authService.login(
                new LoginRequest("aba-cliente@teste.com", "Senha@123", "CLIENT"), "127.0.0.1", "teste").getUser().getId());
        assertEquals(cuidadorId, authService.login(
                new LoginRequest("aba-cuidador@teste.com", "Senha@123", "CAREGIVER"), "127.0.0.1", "teste").getUser().getId());
    }

    @Test
    void abaAdminAceitaPerfisInternosEUserTypeNormalizado() {
        Integer adminId = criarAdmin("aba-admin@teste.com", "12345678909");

        AuthResponse login = authService.login(
                new LoginRequest("aba-admin@teste.com", "Senha@123", "admin"), "127.0.0.1", "teste");
        assertEquals(adminId, login.getUser().getId());
        assertTrue(login.getUser().getPerfis().contains("admin"));

        // usuário multi-perfil também pode entrar pela aba cliente
        AuthResponse comoCliente = authService.login(
                new LoginRequest("aba-admin@teste.com", "Senha@123", "CLIENT"), "127.0.0.1", "teste");
        assertEquals(adminId, comoCliente.getUser().getId());

        // campo ausente mantém compatibilidade com clientes antigos
        assertNotNull(authService.login(
                new LoginRequest("aba-admin@teste.com", "Senha@123", null), "127.0.0.1", "teste").getAccessToken());
    }

    @Test
    void loginUnicoSemUserTypeAceitaClienteECuidador() {
        Integer clienteId = usuarioService.registerCliente(cadastro("unico-cliente@teste.com", "52998224725")).getId();

        CuidadorRegisterRequest cg = new CuidadorRegisterRequest();
        cg.setNome("Ana Login Único");
        cg.setEmail("unico-cuidador@teste.com");
        cg.setCpf("11144477735");
        cg.setSenha("Senha@123");
        cg.setConsentimentoLgpd(true);
        cg.setPrecoHora(new java.math.BigDecimal("45.00"));
        Integer cuidadorId = usuarioService.registerCuidador(cg).getId();

        AuthResponse cliente = authService.login(
                new LoginRequest("unico-cliente@teste.com", "Senha@123", null), "127.0.0.1", "teste");
        assertEquals(clienteId, cliente.getUser().getId());
        assertEquals(List.of("cliente"), cliente.getUser().getPerfis());

        AuthResponse cuidador = authService.login(
                new LoginRequest("unico-cuidador@teste.com", "Senha@123", null), "127.0.0.1", "teste");
        assertEquals(cuidadorId, cuidador.getUser().getId());
        assertTrue(cuidador.getUser().getPerfis().contains("profissional"));
    }

    @Test
    void resetSenhaTrocaSenhaEDerrubaSessao() {
        var criado = usuarioService.registerCliente(cadastro("reset@teste.com", "52998224725"));
        AuthResponse login = authService.login(
                new LoginRequest("reset@teste.com", "Senha@123", "CLIENT"), "127.0.0.1", "teste");
        assertNotNull(login.getRefreshToken());

        String token = authService.requestPasswordReset("reset@teste.com");
        assertNotNull(token);

        authService.confirmPasswordReset(token, "Nova@123");

        // senha antiga não entra mais; nova entra
        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("reset@teste.com", "Senha@123", "CLIENT"), null, null));
        AuthResponse relogin = authService.login(
                new LoginRequest("reset@teste.com", "Nova@123", "CLIENT"), null, null);
        assertEquals(criado.getId(), relogin.getUser().getId());

        // refresh da sessão anterior foi derrubado
        assertThrows(UnauthorizedException.class, () ->
                authService.refresh(login.getRefreshToken(), null, null));

        // token de uso único — reutilizar falha
        assertThrows(UnauthorizedException.class, () ->
                authService.confirmPasswordReset(token, "Outra@123"));
    }

    @Test
    void resetParaInexistenteNaoRevelaNada() {
        assertNull(authService.requestPasswordReset("fantasma@teste.com"));
        assertThrows(UnauthorizedException.class, () ->
                authService.confirmPasswordReset("token-qualquer", "Nova@123"));
    }

    @Test
    void cadastroCuidadorSalvaEspecialidadesDoCatalogo() {
        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
        req.setNome("Cuidadora Especialidades");
        req.setEmail("especialidades@teste.com");
        req.setCpf("11144477735");
        req.setSenha("Senha@123");
        req.setConsentimentoLgpd(true);
        req.setEspecialidades(List.of("Idoso", "Cuidados paliativos", "  "));

        var criado = usuarioService.registerCuidador(req);
        var nomes = usuarioEspecialidades.findNomesByUsuarioId(criado.getId());

        assertEquals(2, nomes.size());
        assertTrue(nomes.contains("Idoso"));
        assertTrue(nomes.contains("Cuidados paliativos"));
    }
}
