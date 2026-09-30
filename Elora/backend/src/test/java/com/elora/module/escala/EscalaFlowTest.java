package com.elora.module.escala;

import com.elora.common.exception.BusinessException;
import com.elora.common.exception.ResourceNotFoundException;
import com.elora.module.escala.dto.AtualizarEscalaRequest;
import com.elora.module.escala.dto.EscalaResponse;
import com.elora.module.escala.dto.GerarEscalaRequest;
import com.elora.module.escala.enums.Periodo;
import com.elora.module.escala.enums.StatusEscala;
import com.elora.module.escala.service.EscalaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo real sobre H2 (create-drop): gerar, idempotência, recorte por
 * período, máquina de estados e validações. Rollback ao final.
 *
 * Nota: no H2 gerado pelas entities não há a FK para contrato (o módulo
 * contrato não existe aqui) — no Postgres real, contrato inexistente
 * vira 422 via DataIntegrityViolation (coberto no service).
 */
@SpringBootTest
@Transactional
class EscalaFlowTest {

    @Autowired
    private EscalaService escalaService;

    private GerarEscalaRequest gerar(Integer contratoId, LocalDate inicio, LocalDate fim, Periodo... periodos) {
        return new GerarEscalaRequest(contratoId, inicio, fim, List.of(periodos));
    }

    @Test
    void gerarCriaUmTurnoPorDiaEPeriodo() {
        List<EscalaResponse> criadas = escalaService.gerar(
                gerar(1, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4),
                        Periodo.matutino, Periodo.vespertino));
        assertEquals(6, criadas.size());
        assertTrue(criadas.stream().allMatch(t -> t.getStatus() == StatusEscala.prevista));
        assertEquals(6, escalaService.listar(1, null, null).size());
    }

    @Test
    void gerarEIdempotente() {
        var req = gerar(7, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), Periodo.noturno);
        assertEquals(2, escalaService.gerar(req).size());
        assertTrue(escalaService.gerar(req).isEmpty()); // segunda vez não duplica
        assertEquals(2, escalaService.listar(7, null, null).size());
    }

    @Test
    void listarRecortaPorPeriodo() {
        escalaService.gerar(gerar(9, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), Periodo.matutino));
        var recorte = escalaService.listar(9, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));
        assertEquals(3, recorte.size());
        assertTrue(recorte.stream().allMatch(t ->
                !t.getData().isBefore(LocalDate.of(2026, 5, 1))
                        && !t.getData().isAfter(LocalDate.of(2026, 5, 3))));
    }

    @Test
    void estadosFinaisSaoImutaveis() {
        Integer id = escalaService.gerar(
                gerar(11, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), Periodo.matutino))
                .get(0).getId();
        var feita = escalaService.atualizarStatus(id, new AtualizarEscalaRequest(StatusEscala.executada));
        assertEquals(StatusEscala.executada, feita.getStatus());
        assertThrows(BusinessException.class, () ->
                escalaService.atualizarStatus(id, new AtualizarEscalaRequest(StatusEscala.faltou)));
        assertThrows(ResourceNotFoundException.class, () ->
                escalaService.atualizarStatus(99999, new AtualizarEscalaRequest(StatusEscala.executada)));
    }

    @Test
    void validacoesDao422() {
        assertThrows(BusinessException.class, () -> escalaService.gerar(
                gerar(1, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 1), Periodo.matutino)));
        assertThrows(BusinessException.class, () -> escalaService.gerar(
                gerar(1, LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), Periodo.matutino)));
    }
}
