package com.elora.module.juridico;

import com.elora.module.contrato.dto.CriarContratoRequest;
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
import com.elora.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Valida rotas, envelope ApiResponse e isolamento por papel (cliente × equipe). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JuridicoControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService jwt;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private UsuarioPerfilRepository usuarioPerfilRepository;
    @Autowired private ContratoService contratoService;

    private Integer cli;
    private Integer prof;
    private Integer admin;
    private Integer contratoId;
    private String cliToken;
    private String adminToken;

    @BeforeEach
    void setup() {
        perfil("cliente");
        perfil("profissional");
        perfil("admin");
        cli = cliente("cli-ctrl@teste.com", "529982247");
        prof = cuidador("prof-ctrl@teste.com", "987654321");
        admin = cliente("adm-ctrl@teste.com", "111444777");
        vinculaPerfil(admin, "admin");
        contratoId = contrato(cli, prof);
        cliToken = jwt.generate(cli.toString(), Map.of("perfis", List.of("cliente")));
        adminToken = jwt.generate(admin.toString(), Map.of("perfis", List.of("admin")));
    }

    @Test
    void clienteCriaEListaAsPropriasSemVerFila() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "denunciadoId", prof,
                "contratoId", contratoId,
                "motivo", "conduta",
                "descricao", "Descricao detalhada do ocorrido com mais de dez caracteres."));

        String criada = mvc.perform(post("/juridico/denuncias")
                        .header("Authorization", "Bearer " + cliToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("aberta"))
                .andReturn().getResponse().getContentAsString();
        int id = mapper.readTree(criada).path("data").path("id").asInt();

        mvc.perform(get("/juridico/denuncias/minhas")
                        .header("Authorization", "Bearer " + cliToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id));

        mvc.perform(get("/juridico/denuncias/" + id)
                        .header("Authorization", "Bearer " + cliToken))
                .andExpect(status().isOk());

        // A dona da denúncia vê a própria; a fila completa é só da equipe.
        mvc.perform(get("/juridico/denuncias")
                        .header("Authorization", "Bearer " + cliToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void equipeListaEAtualizaStatus() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "denunciadoId", prof, "motivo", "conduta",
                "descricao", "Descricao detalhada do ocorrido com mais de dez caracteres."));
        String criada = mvc.perform(post("/juridico/analise")
                        .header("Authorization", "Bearer " + cliToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = mapper.readTree(criada).path("data").path("id").asInt();

        mvc.perform(get("/juridico/denuncias")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id));

        mvc.perform(patch("/juridico/denuncias/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"em_analise\",\"parecer\":\"Assumida\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("em_analise"))
                .andExpect(jsonPath("$.data.parecer").value("Assumida"));
    }

    @Test
    void semTokenNaoAcessa() throws Exception {
        mvc.perform(get("/juridico/denuncias")).andExpect(status().isUnauthorized());
        mvc.perform(get("/juridico/denuncias/minhas")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

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
