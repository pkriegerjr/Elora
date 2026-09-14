package com.elora.module.relatorio;

import com.elora.common.exception.ForbiddenException;
import com.elora.module.usuario.dto.ClienteRegisterRequest;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.entity.Usuario;
import com.elora.module.usuario.entity.UsuarioPerfil;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.repository.UsuarioPerfilRepository;
import com.elora.module.usuario.repository.UsuarioRepository;
import com.elora.module.usuario.service.UsuarioService;
import com.elora.module.relatorio.service.RelatorioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Relatórios fim a fim sobre H2: tabelas dos outros módulos criadas à mão
 * (só colunas lidas), usuários via UsuarioService. Rollback ao final.
 */
@SpringBootTest
@Transactional
class RelatorioFlowTest {

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JdbcTemplate jdbc;

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
        jdbc.execute("CREATE TABLE IF NOT EXISTS pagamento (id_pagamento INT AUTO_INCREMENT PRIMARY KEY, "
                + "valor_bruto DECIMAL(12,2), valor_taxa DECIMAL(12,2), valor_liquido DECIMAL(12,2), "
                + "metodo VARCHAR(10), status VARCHAR(20), criado_em DATETIME)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS repasse (id_repasse INT AUTO_INCREMENT PRIMARY KEY, "
                + "valor DECIMAL(12,2), status VARCHAR(20))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contrato (id_contrato INT AUTO_INCREMENT PRIMARY KEY, "
                + "status VARCHAR(20), valor_hora DECIMAL(10,2), criado_em DATETIME)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS avaliacao (id_avaliacao INT AUTO_INCREMENT PRIMARY KEY, nota INT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS denuncia (id_denuncia INT AUTO_INCREMENT PRIMARY KEY, status VARCHAR(20))");
        // DDL do H2 não sofre rollback: limpa para cada teste ser isolado.
        jdbc.execute("DELETE FROM pagamento");
        jdbc.execute("DELETE FROM repasse");
        jdbc.execute("DELETE FROM contrato");
        jdbc.execute("DELETE FROM avaliacao");
        jdbc.execute("DELETE FROM denuncia");

        perfil("cliente");
        perfil("admin");
        clienteId = usuarioService.registerCliente(cadastro("cli@teste.com", "52998224725")).getId();
        adminId = usuarioService.registerCliente(cadastro("adm@teste.com", "11144477735")).getId();

        Usuario admin = usuarioRepository.findById(adminId).orElseThrow();
        UsuarioPerfil vinculo = new UsuarioPerfil();
        vinculo.setUsuario(admin);
        vinculo.setPerfil(perfilRepository.findByNome("admin").orElseThrow());
        usuarioPerfilRepository.save(vinculo);

        jdbc.update("INSERT INTO pagamento (valor_bruto, valor_taxa, valor_liquido, metodo, status, criado_em)"
                + " VALUES (100.00, 10.00, 90.00, 'pix', 'aprovado', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO pagamento (valor_bruto, valor_taxa, valor_liquido, metodo, status, criado_em)"
                + " VALUES (200.00, 20.00, 180.00, 'cartao', 'pendente', DATEADD('DAY', -40, CURRENT_TIMESTAMP))");
        jdbc.update("INSERT INTO repasse (valor, status) VALUES (90.00, 'pendente')");
        jdbc.update("INSERT INTO contrato (status, valor_hora, criado_em) VALUES ('ativo', 50.00, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO avaliacao (nota) VALUES (5)");
        jdbc.update("INSERT INTO avaliacao (nota) VALUES (3)");
        jdbc.update("INSERT INTO denuncia (status) VALUES ('aberta')");
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

    @Test
    void resumoConsolida() {
        var r = relatorioService.resumo(adminId);
        assertEquals(2, r.getTotalUsuarios());
        assertEquals(2, r.getUsuariosAtivos());
        assertEquals(1, r.getContratosAtivos());
        assertEquals(1, r.getPagamentosPendentes());
        assertEquals(1, r.getRepassesPendentes());
        assertEquals(1, r.getDenunciasAbertas());
    }

    @Test
    void financeiroRespeitaPeriodo() {
        var todos = relatorioService.financeiro(adminId, null, null);
        assertEquals(2, todos.getTotalPagamentos());

        var recente = relatorioService.financeiro(adminId, LocalDate.now().minusDays(7), LocalDate.now());
        assertEquals(1, recente.getTotalPagamentos());
        assertEquals(1L, recente.getPorMetodo().get("pix"));
    }

    @Test
    void demaisRelatorios() {
        assertEquals(1, relatorioService.contratos(adminId, null, null).getTotal());
        assertEquals(2, relatorioService.avaliacoes(adminId).getTotal());
        assertEquals(1, relatorioService.denuncias(adminId).getTotal());
        assertEquals(2, relatorioService.usuarios(adminId, null, null).getTotal());
    }

    @Test
    void naoStaffNaoVe() {
        assertThrows(ForbiddenException.class, () -> relatorioService.resumo(clienteId));
        assertThrows(ForbiddenException.class, () -> relatorioService.financeiro(clienteId, null, null));
    }
}
