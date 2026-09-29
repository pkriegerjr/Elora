package com.elora.module.contrato;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.dto.AtualizarStatusRequest;
import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.service.ContratoService;
import com.elora.module.notificacao.service.NotificacaoService;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2: rascunho → funil → assinaturas → ativo (libera
 * pagamento) + escopo + regras de exclusão. Rollback ao final.
 */
@SpringBootTest
@Transactional
class ContratoFlowTest {

    @Autowired
    private ContratoService contratoService;

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

    private Integer clienteId;
    private Integer profissionalId;
    private Integer outroClienteId;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");

        clienteId = usuarioService.registerCliente(cadastro("cli@teste.com", "52998224725")).getId();
        profissionalId = usuarioService.registerCliente(cadastro("pro@teste.com", "11144477735")).getId();
        vincular(profissionalId, "profissional");
        outroClienteId = usuarioService.registerCliente(cadastro("outro@teste.com", "12345678062")).getId();
    }

    @Test
    void funilCompletoComAssinaturaAtiva() {
        ContratoResponse c = contratoService.criar(clienteId, novoContrato(), "127.0.0.1");
        assertEquals(StatusContrato.rascunho, c.getStatus());
        assertTrue(c.getCodigo().startsWith("ELO-"));
        assertEquals(1, notificacaoService.contarNaoLidas(profissionalId));

        avancar(clienteId, c.getId(), StatusContrato.proposta);
        avancar(clienteId, c.getId(), StatusContrato.negociacao);
        avancar(clienteId, c.getId(), StatusContrato.aguard_assinatura);

        ContratoResponse aposCliente = contratoService.assinar(clienteId, c.getId(), null);
        assertEquals(StatusContrato.aguard_assinatura, aposCliente.getStatus());
        assertEquals(1, aposCliente.getAssinaturas().size());

        ContratoResponse ativo = contratoService.assinar(profissionalId, c.getId(), null);
        assertEquals(StatusContrato.ativo, ativo.getStatus());
        assertEquals(2, ativo.getAssinaturas().size());
    }

    @Test
    void transicaoInvalida() {
        ContratoResponse c = contratoService.criar(clienteId, novoContrato(), "127.0.0.1");
        AtualizarStatusRequest pulo = new AtualizarStatusRequest();
        pulo.setStatus(StatusContrato.ativo);
        assertThrows(BusinessException.class,
                () -> contratoService.atualizarStatus(clienteId, c.getId(), pulo.getStatus(), null));
    }

    @Test
    void escopo() {
        ContratoResponse c = contratoService.criar(clienteId, novoContrato(), "127.0.0.1");
        assertThrows(ResourceNotFoundException.class,
                () -> contratoService.buscarPorId(outroClienteId, c.getId()));
        assertTrue(contratoService.meusContratos(outroClienteId, null).isEmpty());
        assertEquals(1, contratoService.meusContratos(profissionalId, "profissional").size());
    }

    @Test
    void excluirSoRascunho() {
        ContratoResponse rascunho = contratoService.criar(clienteId, novoContrato(), "127.0.0.1");
        avancar(clienteId, rascunho.getId(), StatusContrato.proposta);
        assertThrows(BusinessException.class,
                () -> contratoService.excluir(clienteId, rascunho.getId(), null));
        ContratoResponse outro = contratoService.criar(clienteId, novoContrato(), "127.0.0.1");
        contratoService.excluir(clienteId, outro.getId(), null);
        assertThrows(ResourceNotFoundException.class,
                () -> contratoService.buscarPorId(clienteId, outro.getId()));
    }

    @Test
    void criacaoInvalida() {
        CriarContratoRequest mesmo = novoContrato();
        mesmo.setProfissionalId(clienteId);
        assertThrows(BusinessException.class, () -> contratoService.criar(clienteId, mesmo, "127.0.0.1"));

        CriarContratoRequest semPerfil = novoContrato();
        semPerfil.setProfissionalId(outroClienteId);
        assertThrows(BusinessException.class, () -> contratoService.criar(clienteId, semPerfil, "127.0.0.1"));

        CriarContratoRequest datas = novoContrato();
        datas.setDataInicio(java.time.LocalDate.of(2026, 12, 10));
        datas.setDataFim(java.time.LocalDate.of(2026, 12, 1));
        assertThrows(BusinessException.class, () -> contratoService.criar(clienteId, datas, "127.0.0.1"));
    }

    @Test
    void assinaturaDuplicadaEForaDeHora() {
        ContratoResponse c = contratoService.criar(clienteId, novoContrato(), "127.0.0.1");
        assertThrows(BusinessException.class, () -> contratoService.assinar(clienteId, c.getId(), null));
        avancar(clienteId, c.getId(), StatusContrato.proposta);
        avancar(clienteId, c.getId(), StatusContrato.negociacao);
        avancar(clienteId, c.getId(), StatusContrato.aguard_assinatura);
        contratoService.assinar(clienteId, c.getId(), null);
        assertThrows(BusinessException.class, () -> contratoService.assinar(clienteId, c.getId(), null));
    }

    // ------------------------------------------------------------------

    private ContratoResponse avancar(Integer viewerId, Integer id, StatusContrato status) {
        AtualizarStatusRequest req = new AtualizarStatusRequest();
        req.setStatus(status);
        return contratoService.atualizarStatus(viewerId, id, req.getStatus(), null);
    }

    private CriarContratoRequest novoContrato() {
        CriarContratoRequest req = new CriarContratoRequest();
        req.setClienteId(clienteId);
        req.setProfissionalId(profissionalId);
        req.setTitulo("Cuidado semanal");
        req.setValorHora(new BigDecimal("50.00"));
        req.setValorTotal(new BigDecimal("800.00"));
        return req;
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

    private void perfil(String nome) {
        if (perfilRepository.findByNome(nome).isEmpty()) {
            Perfil p = new Perfil();
            p.setNome(nome);
            p.setDescricao("seed de teste");
            perfilRepository.save(p);
        }
    }

    private void vincular(Integer usuarioId, String perfil) {
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        UsuarioPerfil v = new UsuarioPerfil();
        v.setUsuario(u);
        v.setPerfil(perfilRepository.findByNome(perfil).orElseThrow());
        usuarioPerfilRepository.save(v);
    }
}
