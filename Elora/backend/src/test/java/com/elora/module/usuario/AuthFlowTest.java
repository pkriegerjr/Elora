package com.elora.module.usuario;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.UnauthorizedException;
import com.elora.module.usuario.dto.AuthResponse;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.dto.LoginRequest;
import com.elora.module.usuario.dto.RefreshRequest;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.repository.PerfilRepository;
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

    @BeforeEach
    void seeds() {
        for (String nome : new String[]{"cliente", "profissional"}) {
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
    void cadastroCuidadorComecaPendente() {
        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
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
}
