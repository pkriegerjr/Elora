package com.elora.module.escala;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.escala.dto.AtualizarEscalaRequest;
import com.elora.module.escala.dto.CriarEscalaRequest;
import com.elora.module.escala.dto.DefinirDisponibilidadeRequest;
import com.elora.module.escala.dto.EscalaResponse;
import com.elora.module.escala.enums.PeriodoTurno;
import com.elora.module.escala.enums.StatusDisponibilidade;
import com.elora.module.escala.enums.StatusEscala;
import com.elora.module.escala.service.EscalaService;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2: turno → alerta à outra parte → transição →
 * disponibilidade (upsert). Rollback ao final — não suja nada.
 */
@SpringBootTest
@Transactional
class EscalaFlowTest {

    @Autowired
    private EscalaService escalaService;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ContratoRepository contratos;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioPerfilRepository usuarioPerfilRepository;

    private Integer clienteId;
    private Integer profissionalId;
    private Integer outroClienteId;
    private Integer contratoId;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");

        clienteId = usuarioService.registerCliente(cadastro("cli@teste.com", "52998224725")).getId();
        profissionalId = usuarioService.registerCliente(cadastro("pro@teste.com", "11144477735")).getId();
        vincular(profissionalId, "profissional");
        outroClienteId = usuarioService.registerCliente(cadastro("outro@teste.com", "12345678062")).getId();

        Contrato c = new Contrato();
        c.setCodigo("ELO-E-" + System.nanoTime());
        c.setCliente(usuarioRepository.findById(clienteId).orElseThrow());
        c.setProfissional(usuarioRepository.findById(profissionalId).orElseThrow());
        c.setTitulo("Cuidado semanal");
        c.setValorHora(new BigDecimal("50.00"));
        c.setStatus(com.elora.module.contrato.enums.StatusContrato.ativo);
        contratoId = contratos.save(c).getId();
    }

    @Test
    void fluxoCriarAlterarComAlerta() {
        EscalaResponse criada = escalaService.criarEscala(clienteId, novaEscala(), "127.0.0.1");
        assertEquals(StatusEscala.prevista, criada.getStatus());
        assertEquals(1, notificacaoService.contarNaoLidas(profissionalId));

        AtualizarEscalaRequest alt = new AtualizarEscalaRequest();
        alt.setStatus(StatusEscala.executada);
        EscalaResponse feita = escalaService.atualizarEscala(profissionalId, criada.getId(), alt, null);
        assertEquals(StatusEscala.executada, feita.getStatus());
        assertEquals(1, notificacaoService.contarNaoLidas(clienteId));

        AtualizarEscalaRequest trava = new AtualizarEscalaRequest();
        trava.setStatus(StatusEscala.cancelada);
        assertThrows(BusinessException.class,
                () -> escalaService.atualizarEscala(clienteId, criada.getId(), trava, null));
    }

    @Test
    void turnoDuplicadoNaoPassa() {
        escalaService.criarEscala(clienteId, novaEscala(), null);
        assertThrows(BusinessException.class,
                () -> escalaService.criarEscala(clienteId, novaEscala(), null));
    }

    @Test
    void estranhoNaoEscreveNemLe() {
        assertThrows(ForbiddenException.class,
                () -> escalaService.criarEscala(outroClienteId, novaEscala(), null));
        assertThrows(ForbiddenException.class,
                () -> escalaService.listarPorContrato(outroClienteId, contratoId, null, null));
        assertThrows(ForbiddenException.class,
                () -> escalaService.listarPorProfissional(outroClienteId, profissionalId, null, null));
        assertTrue(escalaService.listarPorContrato(clienteId, contratoId, null, null).isEmpty());
    }

    @Test
    void disponibilidadeUpsert() {
        var d1 = escalaService.definirDisponibilidade(profissionalId, disp(null, "2026-10-05",
                PeriodoTurno.matutino, StatusDisponibilidade.disponivel), null);
        var d2 = escalaService.definirDisponibilidade(profissionalId, disp(null, "2026-10-05",
                PeriodoTurno.matutino, StatusDisponibilidade.indisponivel), null);
        assertEquals(d1.getId(), d2.getId());
        assertEquals(StatusDisponibilidade.indisponivel, d2.getStatus());
        assertEquals(1, escalaService.listarDisponibilidade(profissionalId, profissionalId, null, null).size());
    }

    @Test
    void clienteNaoDefineDisponibilidadePropria() {
        assertThrows(BusinessException.class,
                () -> escalaService.definirDisponibilidade(clienteId,
                        disp(null, "2026-10-06", PeriodoTurno.vespertino, null), null));
    }

    // ------------------------------------------------------------------

    private CriarEscalaRequest novaEscala() {
        CriarEscalaRequest req = new CriarEscalaRequest();
        req.setContratoId(contratoId);
        req.setData(LocalDate.of(2026, 10, 6));
        req.setPeriodo(PeriodoTurno.matutino);
        return req;
    }

    private DefinirDisponibilidadeRequest disp(Integer profissionalId, String data,
                                              PeriodoTurno periodo, StatusDisponibilidade status) {
        DefinirDisponibilidadeRequest req = new DefinirDisponibilidadeRequest();
        req.setProfissionalId(profissionalId);
        req.setData(LocalDate.parse(data));
        req.setPeriodo(periodo);
        req.setStatus(status);
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
