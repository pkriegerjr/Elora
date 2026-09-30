package com.elora.module.pagamento;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.pagamento.dto.AtualizarTaxaRequest;
import com.elora.module.pagamento.dto.CriarPagamentoRequest;
import com.elora.module.pagamento.dto.CriarTaxaRequest;
import com.elora.module.pagamento.dto.EstornarRequest;
import com.elora.module.pagamento.dto.PagarRequest;
import com.elora.module.pagamento.dto.PagamentoResponse;
import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.enums.StatusPagamento;
import com.elora.module.pagamento.enums.StatusRepasse;
import com.elora.module.pagamento.service.PagamentoService;
import com.elora.module.pagamento.service.TaxaServicoService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2: taxa → criar (idempotente) → pagar (gateway mock) →
 * repasse → estorno. Rollback ao final — não suja nada.
 */
@SpringBootTest
@Transactional
class PagamentoFlowTest {

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private TaxaServicoService taxaService;

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
    private Integer financeiroId;
    private Integer outroClienteId;
    private Integer contratoId;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");
        perfil("admin");
        perfil("financeiro");

        clienteId = usuarioService.registerCliente(cadastro("cli@teste.com", "52998224725")).getId();
        profissionalId = usuarioService.registerCliente(cadastro("pro@teste.com", "11144477735")).getId();
        vincular(profissionalId, "profissional");
        Integer adminId = usuarioService.registerCliente(cadastro("fin@teste.com", "98765432100")).getId();
        vincular(adminId, "financeiro");
        financeiroId = adminId;
        outroClienteId = usuarioService.registerCliente(cadastro("outro@teste.com", "12345678062")).getId();

        Contrato c = new Contrato();
        c.setCodigo("ELO-T-" + System.nanoTime());
        c.setCliente(usuarioRepository.findById(clienteId).orElseThrow());
        c.setProfissional(usuarioRepository.findById(profissionalId).orElseThrow());
        c.setTitulo("Cuidado semanal");
        c.setValorHora(new BigDecimal("50.00"));
        c.setStatus(com.elora.module.contrato.enums.StatusContrato.ativo);
        contratoId = contratos.save(c).getId();

        CriarTaxaRequest taxa = new CriarTaxaRequest();
        taxa.setNome("Taxa padrão Elora");
        taxa.setPercentual(new BigDecimal("10.00"));
        taxa.setValorFixo(BigDecimal.ZERO);
        taxa.setVigenteDe(LocalDate.now());
        taxaService.criar(financeiroId, taxa, "127.0.0.1");
    }

    @Test
    void fluxoCriarPagarEstornar() {
        PagamentoResponse criado = pagamentoService.criar(clienteId, novoPagamento("idem-1"), "127.0.0.1");
        assertEquals(StatusPagamento.pendente, criado.getStatus());
        assertEquals(new BigDecimal("20.00"), criado.getValorTaxa());
        assertEquals(new BigDecimal("180.00"), criado.getValorLiquido());
        assertNotNull(criado.getIdempotencyKey());

        PagamentoResponse aprovado = pagamentoService.pagar(clienteId, criado.getId(), semToken(), "127.0.0.1");
        assertEquals(StatusPagamento.aprovado, aprovado.getStatus());
        assertNotNull(aprovado.getGatewayId());

        var repasses = pagamentoService.listarRepasses(profissionalId);
        assertEquals(1, repasses.size());
        assertEquals(new BigDecimal("180.00"), repasses.get(0).getValor());
        assertEquals(StatusRepasse.pendente, repasses.get(0).getStatus());

        PagamentoResponse estornado = pagamentoService.estornar(financeiroId, criado.getId(), new EstornarRequest(), "127.0.0.1");
        assertEquals(StatusPagamento.estornado, estornado.getStatus());
        assertEquals(StatusRepasse.falha,
                pagamentoService.listarRepasses(profissionalId).get(0).getStatus());
    }

    @Test
    void idempotenciaMesmaChaveNaoDuplica() {
        PagamentoResponse p1 = pagamentoService.criar(clienteId, novoPagamento("idem-2"), null);
        PagamentoResponse p2 = pagamentoService.criar(clienteId, novoPagamento("idem-2"), null);
        assertEquals(p1.getId(), p2.getId());
        assertEquals(1, pagamentoService.listar(clienteId, null).size());
    }

    @Test
    void transicoesInvalidas() {
        PagamentoResponse p = pagamentoService.criar(clienteId, novoPagamento("idem-3"), null);
        assertThrows(BusinessException.class,
                () -> pagamentoService.estornar(financeiroId, p.getId(), new EstornarRequest(), null));
        pagamentoService.pagar(clienteId, p.getId(), semToken(), null);
        assertThrows(BusinessException.class,
                () -> pagamentoService.pagar(clienteId, p.getId(), semToken(), null));
    }

    @Test
    void recusaGatewayNaoGeraRepasse() {
        PagamentoResponse p = pagamentoService.criar(clienteId, novoPagamento("idem-4"), null);
        PagarRequest recusa = new PagarRequest();
        recusa.setGatewayToken("tok_recusado");
        PagamentoResponse recusado = pagamentoService.pagar(clienteId, p.getId(), recusa, null);
        assertEquals(StatusPagamento.recusado, recusado.getStatus());
        assertTrue(pagamentoService.listarRepasses(profissionalId).isEmpty());
    }

    @Test
    void gatewayIndisponivelMantemPendente() {
        PagamentoResponse p = pagamentoService.criar(clienteId, novoPagamento("idem-6"), null);
        PagarRequest erro = new PagarRequest();
        erro.setGatewayToken("tok_erro");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagamentoService.pagar(clienteId, p.getId(), erro, null));
        assertTrue(ex.getMessage().contains("indispon"));
        assertEquals(StatusPagamento.pendente,
                pagamentoService.buscarPorId(clienteId, p.getId()).getStatus());
        assertTrue(pagamentoService.listarRepasses(profissionalId).isEmpty());
    }

    @Test
    void permissaoTaxas() {        CriarTaxaRequest taxa = new CriarTaxaRequest();
        taxa.setNome("Indevida");
        taxa.setPercentual(new BigDecimal("5.00"));
        taxa.setVigenteDe(LocalDate.now());
        assertThrows(ForbiddenException.class, () -> taxaService.criar(clienteId, taxa, null));
        assertThrows(ForbiddenException.class, () -> taxaService.listar(clienteId));
        assertThrows(ForbiddenException.class,
                () -> taxaService.atualizar(clienteId, 1, new AtualizarTaxaRequest(), null));
    }

    @Test
    void visibilidadePorPapel() {
        PagamentoResponse p = pagamentoService.criar(clienteId, novoPagamento("idem-5"), null);
        assertEquals(1, pagamentoService.listar(profissionalId, null).size());
        assertTrue(pagamentoService.listar(outroClienteId, null).isEmpty());
        assertThrows(ForbiddenException.class,
                () -> pagamentoService.buscarPorId(outroClienteId, p.getId()));
        assertEquals(1, pagamentoService.listar(financeiroId, null).size());
    }

    // ------------------------------------------------------------------

    private CriarPagamentoRequest novoPagamento(String chave) {
        CriarPagamentoRequest req = new CriarPagamentoRequest();
        req.setContratoId(contratoId);
        req.setValorBruto(new BigDecimal("200.00"));
        req.setMetodo(MetodoPagamento.pix);
        req.setIdempotencyKey(chave);
        return req;
    }

    private PagarRequest semToken() {
        return new PagarRequest();
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
