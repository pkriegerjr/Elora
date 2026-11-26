package com.elora.module.juridico;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.module.avaliacao.dto.CriarAvaliacaoRequest;
import com.elora.module.avaliacao.service.AvaliacaoService;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.enums.StatusContrato;
import com.elora.module.contrato.service.ContratoService;
import com.elora.module.juridico.dto.AtualizarStatusDenunciaRequest;
import com.elora.module.juridico.dto.CriarDenunciaRequest;
import com.elora.module.juridico.dto.DenunciaResponse;
import com.elora.module.juridico.enums.StatusDenuncia;
import com.elora.module.juridico.service.JuridicoService;
import com.elora.module.notificacao.service.NotificacaoService;
import com.elora.module.pagamento.dto.CriarPagamentoRequest;
import com.elora.module.pagamento.enums.MetodoPagamento;
import com.elora.module.pagamento.service.PagamentoService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fluxo real sobre H2: criação persistida, validações, transições e contadores. */
@SpringBootTest
@Transactional
class JuridicoFlowTest {

    @Autowired
    private JuridicoService juridicoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioPerfilRepository usuarioPerfilRepository;

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private AvaliacaoService avaliacaoService;

    private Integer cli;
    private Integer prof;
    private Integer admin;
    private Integer contratoId;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");
        perfil("admin");
        cli = cliente("cli-jur@teste.com", "529982247");
        prof = cuidador("prof-jur@teste.com", "987654321");
        admin = cliente("adm-jur@teste.com", "111444777");
        vinculaPerfil(admin, "admin");
        contratoId = contrato(cli, prof);
    }

    @Test
    void criacaoPersistidaEListagens() {
        DenunciaResponse d = juridicoService.criar(cli, req(prof, contratoId,
                "Conduta inadequada", "Descreve o ocorrido com detalhes suficientes."));

        assertNotNull(d.getId());
        assertEquals(StatusDenuncia.aberta, d.getStatus());
        assertEquals(cli, d.getDenuncianteId());
        assertEquals(prof, d.getDenunciadoId());
        assertEquals(1, juridicoService.listarTodas().size());
        assertEquals(1, juridicoService.minhas(cli).size());
        assertEquals(d.getId(), juridicoService.detalhar(d.getId(), cli).getId());
        // Aviso automático da denúncia vai só para a equipe.
        assertEquals(1, notificacaoService.listar(admin, false, "denuncia", 50).size());
        assertEquals(0, notificacaoService.listar(cli, false, "denuncia", 50).size());
    }

    @Test
    void validacoesDeCriacao() {
        assertThrows(BusinessException.class,
                () -> juridicoService.criar(cli, req(cli, null, "Motivo", "Descricao com dez caracteres")));
        assertThrows(BusinessException.class,
                () -> juridicoService.criar(cli, req(prof, contratoId, "Motivo", "curta")));

        Integer outroCli = cliente("outro-jur@teste.com", "123456789");
        Integer outroContrato = contrato(outroCli, prof);
        assertThrows(BusinessException.class,
                () -> juridicoService.criar(cli, req(prof, outroContrato,
                        "Motivo", "Descricao com detalhes suficientes.")));
    }

    @Test
    void transicoesSomenteDaEquipe() {
        DenunciaResponse d = juridicoService.criar(cli, req(prof, contratoId,
                "Conduta inadequada", "Descreve o ocorrido com detalhes suficientes."));

        assertThrows(ForbiddenException.class,
                () -> juridicoService.atualizarStatus(d.getId(),
                        new AtualizarStatusDenunciaRequest(StatusDenuncia.em_analise, "x"), cli));

        DenunciaResponse emAnalise = juridicoService.atualizarStatus(d.getId(),
                new AtualizarStatusDenunciaRequest(StatusDenuncia.em_analise, "Assumida"), admin);
        assertEquals(StatusDenuncia.em_analise, emAnalise.getStatus());
        assertEquals(admin, emAnalise.getAnalisadoPor());
        assertEquals(1, notificacaoService.listar(cli, false, "denuncia", 50).size());

        DenunciaResponse resolvida = juridicoService.atualizarStatus(d.getId(),
                new AtualizarStatusDenunciaRequest(StatusDenuncia.resolvida, "Procedente"), admin);
        assertEquals(StatusDenuncia.resolvida, resolvida.getStatus());
        assertEquals("Procedente", resolvida.getParecer());
        assertEquals(2, notificacaoService.listar(cli, false, "denuncia", 50).size());
        assertEquals(1, notificacaoService.listar(admin, false, "denuncia", 50).size());

        assertThrows(BusinessException.class,
                () -> juridicoService.atualizarStatus(d.getId(),
                        new AtualizarStatusDenunciaRequest(StatusDenuncia.em_analise, null), admin));

        DenunciaResponse reaberta = juridicoService.atualizarStatus(d.getId(),
                new AtualizarStatusDenunciaRequest(StatusDenuncia.aberta, null), admin);
        assertEquals(StatusDenuncia.aberta, reaberta.getStatus());
        assertEquals(null, reaberta.getAnalisadoPor());
    }

    @Test
    void avisosAutomaticosDeContratoEPagamento() {
        contratoService.atualizarStatus(admin, contratoId, StatusContrato.proposta);
        assertEquals(1, notificacaoService.listar(cli, false, "contrato", 50).size());
        assertEquals(1, notificacaoService.listar(prof, false, "contrato", 50).size());
        assertEquals(0, notificacaoService.listar(admin, false, "contrato", 50).size());

        contratoService.atualizarStatus(admin, contratoId, StatusContrato.negociacao);
        contratoService.atualizarStatus(admin, contratoId, StatusContrato.aguard_assinatura);
        contratoService.atualizarStatus(admin, contratoId, StatusContrato.ativo);
        assertEquals(2, notificacaoService.listar(cli, false, "contrato", 50).size());
        assertEquals(2, notificacaoService.listar(prof, false, "contrato", 50).size());

        var pagamento = pagamentoService.criar(cli, new CriarPagamentoRequest(
                contratoId, new BigDecimal("100.00"), MetodoPagamento.pix, null), "127.0.0.1");
        assertEquals(1, notificacaoService.listar(cli, false, "pagamento", 50).size());
        assertEquals(0, notificacaoService.listar(prof, false, "pagamento", 50).size());
        assertEquals(1, notificacaoService.listar(admin, false, "pagamento", 50).size());

        pagamentoService.pagar(cli, pagamento.getId(), null, "127.0.0.1");
        assertEquals(2, notificacaoService.listar(cli, false, "pagamento", 50).size());
        assertEquals(1, notificacaoService.listar(prof, false, "pagamento", 50).size());

        contratoService.atualizarStatus(admin, contratoId, StatusContrato.em_disputa);
        assertEquals(3, notificacaoService.listar(cli, false, "contrato", 50).size());
        assertEquals(3, notificacaoService.listar(prof, false, "contrato", 50).size());
        assertEquals(1, notificacaoService.listar(admin, false, "contrato", 50).size());

        pagamentoService.estornar(admin, pagamento.getId(), null, "127.0.0.1");
        assertEquals(3, notificacaoService.listar(cli, false, "pagamento", 50).size());
        assertEquals(2, notificacaoService.listar(prof, false, "pagamento", 50).size());
        assertEquals(2, notificacaoService.listar(admin, false, "pagamento", 50).size());
    }

    @Test
    void avisoAutomaticoDeAvaliacao() {
        avaliacaoService.criar(cli, new CriarAvaliacaoRequest(contratoId, 5, "Excelente cuidado"));

        assertEquals(1, notificacaoService.listar(prof, false, "avaliacao", 50).size());
        assertEquals(0, notificacaoService.listar(cli, false, "avaliacao", 50).size());
    }

    @Test
    void painelContaDenunciasReais() {
        juridicoService.criar(cli, req(prof, contratoId,
                "Conduta inadequada", "Descreve o ocorrido com detalhes suficientes."));
        Map<String, Object> painel = juridicoService.painel();

        assertEquals(1L, painel.get("denunciasAbertas"));
        assertEquals(1L, painel.get("analisesPendentes"));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private CriarDenunciaRequest req(Integer denunciadoId, Integer contratoId,
                                     String motivo, String descricao) {
        return new CriarDenunciaRequest(denunciadoId, contratoId, motivo, descricao);
    }

    private void perfil(String nome) {
        if (perfilRepository.findByNome(nome).isEmpty()) {
            Perfil p = new Perfil();
            p.setNome(nome);
            p.setDescricao("seed de teste");
            perfilRepository.save(p);
        }
    }

    private void vinculaPerfil(Integer usuarioId, String nome) {
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        UsuarioPerfil up = new UsuarioPerfil();
        up.setUsuario(u);
        up.setPerfil(perfilRepository.findByNome(nome).orElseThrow());
        usuarioPerfilRepository.save(up);
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
        req.setPrecoHora(new BigDecimal("50.00"));
        return usuarioService.registerCuidador(req).getId();
    }

    private Integer contrato(Integer clienteId, Integer profissionalId) {
        CriarContratoRequest req = new CriarContratoRequest();
        req.setClienteId(clienteId);
        req.setProfissionalId(profissionalId);
        req.setTitulo("Cuidado teste");
        req.setValorHora(new BigDecimal("40.00"));
        return contratoService.criar(clienteId, req).getId();
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
