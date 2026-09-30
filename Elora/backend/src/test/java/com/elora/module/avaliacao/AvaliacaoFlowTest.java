package com.elora.module.avaliacao;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ForbiddenException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.avaliacao.dto.AvaliacoesCuidadorResponse;
import com.elora.module.avaliacao.dto.CriarAvaliacaoRequest;
import com.elora.module.avaliacao.dto.MediaAvaliacaoResponse;
import com.elora.module.avaliacao.entity.Avaliacao;
import com.elora.module.avaliacao.repository.AvaliacaoRepository;
import com.elora.module.avaliacao.service.AvaliacaoService;
import com.elora.module.contrato.dto.CriarContratoRequest;
import com.elora.module.contrato.entity.Contrato;
import com.elora.module.contrato.repository.ContratoRepository;
import com.elora.module.contrato.service.ContratoService;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.module.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2 (padrão NotificacaoFlowTest): cria avaliação válida,
 * notas inválidas, comentário opcional, par único (app + banco), usuários
 * diferentes no mesmo cuidador, média 17/4=4.25, média zerada e 404s.
 * Rollback ao final — não suja nada.
 */
@SpringBootTest
@Transactional
class AvaliacaoFlowTest {

    @Autowired
    private AvaliacaoService avaliacaoService;

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    private Integer cliA;
    private Integer cliB;
    private Integer cliC;
    private Integer cliD;
    private Integer cuidX;
    private Integer cuidY;
    private Integer contratoAX;
    private Integer contratoAY;
    private Integer contratoBX;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");
        cliA = cliente("a@t.com", "123456789");
        cliB = cliente("b@t.com", "987654321");
        cliC = cliente("c@t.com", "135792468");
        cliD = cliente("d@t.com", "246813579");
        cuidX = cuidador("x@t.com", "111222333", new BigDecimal("50.00"));
        cuidY = cuidador("y@t.com", "444555666", new BigDecimal("30.00"));
        contratoAX = contrato(cliA, cuidX);
        contratoAY = contrato(cliA, cuidY);
        contratoBX = contrato(cliB, cuidX);
    }

    @Test
    void criarValida() {
        var r = avaliacaoService.criar(cliA, req(contratoAX, 5, "Excelente profissional"));
        assertEquals(cliA, r.getAvaliadorId());
        assertEquals(cuidX, r.getCuidadorId());
        assertEquals(5, r.getNota());
        assertEquals("Excelente profissional", r.getComentario());
        assertNotNull(r.getCriadoEm());
    }

    @Test
    void notaInvalida() {
        assertThrows(BusinessException.class,
                () -> avaliacaoService.criar(cliA, req(contratoAX, 0, "x")));
        assertThrows(BusinessException.class,
                () -> avaliacaoService.criar(cliA, req(contratoAX, 6, "x")));
    }

    @Test
    void comentarioOpcional() {
        var r = avaliacaoService.criar(cliA, req(contratoAX, 4, null));
        assertNull(r.getComentario());
    }

    @Test
    void parUnicoNaApp() {
        avaliacaoService.criar(cliA, req(contratoAX, 5, "boa"));
        // Segundo contrato entre os MESMOS dois: app barra pelo par (não pelo contrato).
        Integer contratoAX2 = contrato(cliA, cuidX);
        var ex = assertThrows(BusinessException.class,
                () -> avaliacaoService.criar(cliA, req(contratoAX2, 4, "outra")));
        assertTrue(ex.getMessage().contains("já avaliou"));
    }

    @Test
    void parUnicoNoBanco() {
        // Furando o service de propósito: o UNIQUE do banco barra (vira 409 na API).
        Avaliacao a1 = entidade(contratoAX, cliA, cuidX, (short) 5);
        Avaliacao a2 = entidade(contratoAY, cliA, cuidX, (short) 4);
        avaliacaoRepository.saveAndFlush(a1);
        assertThrows(DataIntegrityViolationException.class,
                () -> avaliacaoRepository.saveAndFlush(a2));
    }

    @Test
    void usuariosDiferentesMesmoCuidador() {
        avaliacaoService.criar(cliA, req(contratoAX, 5, "a"));
        var r = avaliacaoService.criar(cliB, req(contratoBX, 4, "b"));
        assertEquals(cuidX, r.getCuidadorId());
    }

    @Test
    void mediaCorreta() {
        avaliacaoService.criar(cliA, req(contratoAX, 5, "a"));
        avaliacaoService.criar(cliB, req(contratoBX, 4, "b"));
        avaliacaoService.criar(cliC, req(contrato(cliC, cuidX), 5, "c"));
        avaliacaoService.criar(cliD, req(contrato(cliD, cuidX), 3, "d"));
        // 5+4+5+3 = 17, total 4 → 4.25
        MediaAvaliacaoResponse m = avaliacaoService.mediaPorCuidador(cuidX);
        assertEquals(17L, m.getSomaNotas());
        assertEquals(4L, m.getTotalAvaliacoes());
        assertEquals(0, new BigDecimal("4.25").compareTo(m.getMedia()));

        AvaliacoesCuidadorResponse lista = avaliacaoService.listarPorCuidador(cuidX);
        assertEquals(4, lista.getAvaliacoes().size());
        assertEquals(4L, lista.getTotalAvaliacoes());
    }

    @Test
    void mediaZeradaSemAvaliacoes() {
        MediaAvaliacaoResponse m = avaliacaoService.mediaPorCuidador(cuidY);
        assertEquals(0L, m.getSomaNotas());
        assertEquals(0L, m.getTotalAvaliacoes());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(m.getMedia()));
    }

    @Test
    void cuidadorInexistente() {
        assertThrows(ResourceNotFoundException.class,
                () -> avaliacaoService.listarPorCuidador(99999));
        assertThrows(ResourceNotFoundException.class,
                () -> avaliacaoService.mediaPorCuidador(99999));
    }

    @Test
    void clienteNaoECuidador() {
        assertThrows(ResourceNotFoundException.class,
                () -> avaliacaoService.listarPorCuidador(cliA));
    }

    @Test
    void naoClienteNaoAvalia() {
        assertThrows(ForbiddenException.class,
                () -> avaliacaoService.criar(cliB, req(contratoAX, 5, "x")));
    }

    @Test
    void contratoInexistente() {
        assertThrows(ResourceNotFoundException.class,
                () -> avaliacaoService.criar(cliA, req(99999, 5, "x")));
    }

    // ------------------------------------------------------------------
    // Apoio: seeds + CPF válido (mod11, espelhando DocumentUtils)
    // ------------------------------------------------------------------

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

    private Integer cuidador(String email, String base9, BigDecimal preco) {
        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
        req.setNome("Cuidador " + email);
        req.setEmail(email);
        req.setCpf(cpf(base9));
        req.setSenha("Senha@123");
        req.setPrecoHora(preco);
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

    private CriarAvaliacaoRequest req(Integer contratoId, Integer nota, String comentario) {
        return new CriarAvaliacaoRequest(contratoId, nota, comentario);
    }

    private Avaliacao entidade(Integer contratoId, Integer avaliadorId, Integer avaliadoId, short nota) {
        Contrato c = contratoRepository.findById(contratoId).orElseThrow();
        Usuario avaliador = usuarioRepository.findById(avaliadorId).orElseThrow();
        Usuario avaliado = usuarioRepository.findById(avaliadoId).orElseThrow();
        Avaliacao a = new Avaliacao();
        a.setContrato(c);
        a.setAvaliador(avaliador);
        a.setAvaliado(avaliado);
        a.setNota(nota);
        return a;
    }

    /** Gera CPF válido a partir de 9 dígitos (mesma conta do DocumentUtils). */
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
