package com.elora.module.contrato;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.contrato.dto.AtualizarContratoRequest;
import com.elora.module.contrato.dto.ContratoResponse;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.service.ContratoService;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2 (padrão NotificacaoFlowTest): criação e regras,
 * ciclo de vida completo do funil, saltos proibidos, edição, exclusão,
 * visibilidade e staff. Rollback ao final.
 */
@SpringBootTest
@Transactional
class ContratoFlowTest {

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioPerfilRepository usuarioPerfilRepository;

    private Integer cli;
    private Integer prof;
    private Integer admin;
    private Integer estranho;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");
        perfil("admin");
        cli = cliente("cli@t.com", "123456789");
        prof = cuidador("prof@t.com", "987654321");
        estranho = cliente("outro@t.com", "135792468");
        admin = cliente("adm@t.com", "246813579");
        Usuario adminEntity = usuarioRepository.findById(admin).orElseThrow();
        UsuarioPerfil vinculo = new UsuarioPerfil();
        vinculo.setUsuario(adminEntity);
        vinculo.setPerfil(perfilRepository.findByNome("admin").orElseThrow());
        usuarioPerfilRepository.save(vinculo);
    }

    @Test
    void criarOk() {
        ContratoResponse c = contratoService.criar(cli, novo(cli, prof));
        assertEquals(StatusContrato.rascunho, c.getStatus());
        assertTrue(c.getCodigo().startsWith("ELO-"));
        assertEquals(cli, c.getCriadoPorId());
        assertNotNull(c.getCriadoEm());
    }

    @Test
    void criarRegras() {
        assertThrows(BusinessException.class, () -> contratoService.criar(cli, novo(cli, cli)));
        assertThrows(BusinessException.class, () -> contratoService.criar(cli, novo(cli, admin)));
        assertThrows(ForbiddenException.class, () -> contratoService.criar(prof, novo(cli, prof)));
        CriarContratoRequest datasRuins = novo(cli, prof);
        datasRuins.setDataFim(LocalDate.now().minusDays(1));
        assertThrows(BusinessException.class, () -> contratoService.criar(cli, datasRuins));
        // staff cria pelo cliente
        assertEquals(admin, contratoService.criar(admin, novo(cli, prof)).getCriadoPorId());
    }

    @Test
    void cicloFeliz() {
        Integer id = contratoService.criar(cli, novo(cli, prof)).getId();
        assertEquals(StatusContrato.proposta, status(id, StatusContrato.proposta));
        assertEquals(StatusContrato.negociacao, status(id, StatusContrato.negociacao));
        assertEquals(StatusContrato.aguard_assinatura, status(id, StatusContrato.aguard_assinatura));
        assertEquals(StatusContrato.ativo, status(id, StatusContrato.ativo));
        assertEquals(StatusContrato.concluido, status(id, StatusContrato.concluido));
    }

    @Test
    void saltosProibidos() {
        Integer id = contratoService.criar(cli, novo(cli, prof)).getId();
        assertThrows(BusinessException.class, () -> status(id, StatusContrato.ativo));
        status(id, StatusContrato.proposta);
        status(id, StatusContrato.aguard_assinatura);
        status(id, StatusContrato.ativo);
        status(id, StatusContrato.em_disputa);
        assertEquals(StatusContrato.rescindido, status(id, StatusContrato.rescindido));
        assertThrows(BusinessException.class, () -> status(id, StatusContrato.proposta));
    }

    @Test
    void cancelamento() {
        Integer id = contratoService.criar(cli, novo(cli, prof)).getId();
        assertEquals(StatusContrato.cancelado, status(id, StatusContrato.cancelado));
    }

    @Test
    void editar() {
        Integer id = contratoService.criar(cli, novo(cli, prof)).getId();
        AtualizarContratoRequest edit = new AtualizarContratoRequest();
        edit.setTitulo("Novo título");
        ContratoResponse atualizado = contratoService.atualizar(cli, id, edit);
        assertEquals("Novo título", atualizado.getTitulo());
        status(id, StatusContrato.proposta);
        status(id, StatusContrato.aguard_assinatura);
        status(id, StatusContrato.ativo);
        status(id, StatusContrato.concluido);
        assertThrows(BusinessException.class, () -> contratoService.atualizar(cli, id, edit));
    }

    @Test
    void excluir() {
        Integer rascunho = contratoService.criar(cli, novo(cli, prof)).getId();
        contratoService.excluir(cli, rascunho);
        assertThrows(ResourceNotFoundException.class, () -> contratoService.buscarPorId(cli, rascunho));
        Integer id = contratoService.criar(cli, novo(cli, prof)).getId();
        status(id, StatusContrato.proposta);
        assertThrows(BusinessException.class, () -> contratoService.excluir(cli, id));
    }

    @Test
    void visibilidade() {
        Integer id = contratoService.criar(cli, novo(cli, prof)).getId();
        assertEquals(id, contratoService.buscarPorId(prof, id).getId());
        assertEquals(id, contratoService.buscarPorId(admin, id).getId());
        assertThrows(ResourceNotFoundException.class, () -> contratoService.buscarPorId(estranho, id));
        assertTrue(contratoService.meusContratos(cli, "cliente").stream()
                .anyMatch(c -> c.getId().equals(id)));
        assertTrue(contratoService.meusContratos(prof, "profissional").stream()
                .anyMatch(c -> c.getId().equals(id)));
        assertTrue(contratoService.meusContratos(estranho, null).isEmpty());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private StatusContrato status(Integer id, StatusContrato novo) {
        return contratoService.atualizarStatus(cli, id, novo).getStatus();
    }

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

    private Integer cuidador(String email, String base9) {
        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
        req.setNome("Cuidador " + email);
        req.setEmail(email);
        req.setCpf(cpf(base9));
        req.setSenha("Senha@123");
        req.setPrecoHora(new BigDecimal("40.00"));
        return usuarioService.registerCuidador(req).getId();
    }

    private CriarContratoRequest novo(Integer clienteId, Integer profissionalId) {
        CriarContratoRequest req = new CriarContratoRequest();
        req.setClienteId(clienteId);
        req.setProfissionalId(profissionalId);
        req.setTitulo("Cuidado teste");
        req.setValorHora(new BigDecimal("40.00"));
        req.setDataInicio(LocalDate.now().plusDays(1));
        req.setDataFim(LocalDate.now().plusDays(10));
        return req;
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
