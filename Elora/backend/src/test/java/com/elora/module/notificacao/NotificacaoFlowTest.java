package com.elora.module.notificacao;

import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.notificacao.dto.CriarNotificacaoRequest;
import com.elora.module.notificacao.dto.NotificacaoResponse;
import com.elora.module.notificacao.dto.PreferenciasRequest;
import com.elora.module.notificacao.service.NotificacaoService;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.UsuarioResponse;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.entity.UsuarioPerfil;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.repository.UsuarioPerfilRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.module.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2: preferências → envio (staff) → leitura → contadores.
 * Rollback ao final — não suja nada.
 */
@SpringBootTest
@Transactional
class NotificacaoFlowTest {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioPerfilRepository usuarioPerfilRepository;

    private Integer adminId;
    private Integer clienteId;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("admin");

        clienteId = usuarioService.registerCliente(cadastro("cli@teste.com", "52998224725")).getId();
        UsuarioResponse admin = usuarioService.registerCliente(cadastro("adm@teste.com", "11144477735"));
        adminId = admin.getId();

        Usuario adminEntity = usuarioRepository.findById(adminId).orElseThrow();
        UsuarioPerfil vinculo = new UsuarioPerfil();
        vinculo.setUsuario(adminEntity);
        vinculo.setPerfil(perfilRepository.findByNome("admin").orElseThrow());
        usuarioPerfilRepository.save(vinculo);
    }

    private void perfil(String nome) {
        if (perfilRepository.findByNome(nome).isEmpty()) {
            Perfil p = new Perfil();
            p.setNome(nome);
            p.setDescricao("seed de teste");
            perfilRepository.save(p);
        }
    }

    private ClienteRegisterRequest cadastro(String email, String cpf) {
        ClienteRegisterRequest req = new ClienteRegisterRequest();
        req.setNome("Teste");
        req.setEmail(email);
        req.setCpf(cpf);
        req.setSenha("Senha@123");
        req.setConsentimentoLgpd(true);
        return req;
    }

    private CriarNotificacaoRequest nova(String titulo, String tipo) {
        CriarNotificacaoRequest req = new CriarNotificacaoRequest();
        req.setDestinatarioId(clienteId);
        req.setTitulo(titulo);
        req.setCorpo("corpo");
        req.setReferenciaTipo(tipo);
        return req;
    }

    @Test
    void preferenciasPadraoEAtualizacao() {
        var prefs = notificacaoService.obterPreferencias(clienteId);
        assertTrue(prefs.getPush());
        assertTrue(prefs.getEmail());
        assertFalse(prefs.getSms());

        var atual = notificacaoService.atualizarPreferencias(clienteId, new PreferenciasRequest(null, null, true));
        assertTrue(atual.getSms());
        assertTrue(atual.getPush()); // parcial: resto preservado
    }

    @Test
    void fluxoLeituraEContadores() {
        notificacaoService.enviar(adminId, nova("Bem-vindo", "sistema"));
        notificacaoService.enviar(adminId, nova("Contrato assinado", "contrato"));

        assertEquals(2, notificacaoService.contarNaoLidas(clienteId));
        assertEquals(2, notificacaoService.listar(clienteId, true, null, 20).size());
        assertEquals(1, notificacaoService.listar(clienteId, true, "contrato", 20).size());

        NotificacaoResponse primeira =
                notificacaoService.listar(clienteId, true, null, 20).get(0);
        var lida = notificacaoService.marcarComoLida(clienteId, primeira.getId(), true);
        assertTrue(lida.getLida());
        assertEquals(1, notificacaoService.contarNaoLidas(clienteId));

        assertEquals(1, notificacaoService.marcarTodasComoLidas(clienteId));
        assertEquals(0, notificacaoService.contarNaoLidas(clienteId));
    }

    @Test
    void excluirRemove() {
        NotificacaoResponse criada = notificacaoService.enviar(adminId, nova("x", null));
        notificacaoService.excluir(clienteId, criada.getId());
        assertEquals(0, notificacaoService.listar(clienteId, false, null, 20).size());
    }

    @Test
    void naoStaffNaoEnvia() {
        assertThrows(ForbiddenException.class, () -> notificacaoService.enviar(clienteId, nova("x", null)));
    }

    @Test
    void lidaDeOutroUsuarioDa404() {
        NotificacaoResponse criada = notificacaoService.enviar(adminId, nova("x", null));
        // admin tenta operar notificação do cliente: não vaza existência
        assertThrows(ResourceNotFoundException.class,
                () -> notificacaoService.marcarComoLida(adminId, criada.getId(), true));
    }

    @Test
    void bulkEnviaParaTodos() {
        CriarNotificacaoRequest base = new CriarNotificacaoRequest();
        base.setTitulo("Aviso geral");
        base.setCorpo("corpo");
        var todas = notificacaoService.enviarEmLote(adminId, List.of(clienteId, adminId), base);
        assertEquals(2, todas.size());
        assertEquals(1, notificacaoService.contarNaoLidas(adminId));
    }
}
