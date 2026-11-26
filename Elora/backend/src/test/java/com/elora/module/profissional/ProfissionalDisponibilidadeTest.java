package com.elora.module.profissional;

import com.elora.module.profissional.repository.DisponibilidadeRepository;
import com.elora.module.profissional.service.ProfissionalService;
import com.elora.module.usuario.dto.CuidadorRegisterRequest;
import com.elora.module.usuario.entity.Perfil;
import com.elora.module.usuario.repository.PerfilRepository;
import com.elora.module.usuario.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A disponibilidade é substituída a cada salvamento. O UNIQUE
 * (usuario_id, data, periodo) não pode barrar a própria substituição.
 */
@SpringBootTest
@Transactional
class ProfissionalDisponibilidadeTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProfissionalService profissionalService;

    @Autowired
    private DisponibilidadeRepository dispRepo;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void seeds() {
        for (String nome : new String[]{"cliente", "profissional", "admin", "moderador", "juridico", "financeiro"}) {
            if (perfilRepository.findByNome(nome).isEmpty()) {
                Perfil p = new Perfil();
                p.setNome(nome);
                p.setDescricao("seed de teste");
                perfilRepository.save(p);
            }
        }
    }

    private Integer cuidador(String email, String cpf) {
        CuidadorRegisterRequest req = new CuidadorRegisterRequest();
        req.setNome("Ana Disponibilidade");
        req.setEmail(email);
        req.setCpf(cpf);
        req.setSenha("Senha@123");
        req.setConsentimentoLgpd(true);
        req.setPrecoHora(new BigDecimal("45.00"));
        return usuarioService.registerCuidador(req).getId();
    }

    private Map<String, String> item(String data, String periodo) {
        return Map.of("data", data, "periodo", periodo);
    }

    @Test
    void salvarDisponibilidadeSegundaVezSubstituiSemConflito() {
        Integer id = cuidador("disp-segunda@teste.com", "11144477735");
        String data = LocalDate.now().plusDays(1).toString();

        profissionalService.salvarDisponibilidade(id, List.of(item(data, "matutino")));
        assertEquals(1, dispRepo.findByUsuarioId(id).size());

        profissionalService.salvarDisponibilidade(id, List.of(item(data, "vespertino")));
        em.flush();
        em.clear();

        var salvos = dispRepo.findByUsuarioId(id);
        assertEquals(1, salvos.size(), "a substituição deve remover o turno anterior");
        assertEquals("vespertino", salvos.get(0).getPeriodo().name());
    }

    @Test
    void payloadComChaveRepetidaGeraApenasUmRegistro() {
        Integer id = cuidador("disp-duplicada@teste.com", "52998224725");
        String data = LocalDate.now().plusDays(2).toString();

        profissionalService.salvarDisponibilidade(id, List.of(
                item(data, "noturno"),
                item(data, "noturno"),
                item(data, "matutino")));
        em.flush();
        em.clear();

        var salvos = dispRepo.findByUsuarioId(id);
        assertEquals(2, salvos.size());
        Set<String> periodos = salvos.stream()
                .map(d -> d.getPeriodo().name())
                .collect(Collectors.toSet());
        assertEquals(Set.of("matutino", "noturno"), periodos);
    }
}
