package com.elora.module.relatorio.service;

import com.elora.module.relatorio.service.RelatorioService.ContratoRow;
import com.elora.module.relatorio.service.RelatorioService.PagamentoRow;
import com.elora.module.relatorio.service.RelatorioService.RepasseRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatorioServiceTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void financeiroAgregaTotaisEMapas() {
        var pags = List.of(
                new PagamentoRow(bd("100.00"), bd("10.00"), bd("90.00"), "pix", "aprovado"),
                new PagamentoRow(bd("200.00"), bd("20.00"), bd("180.00"), "cartao", "pendente"));
        var reps = List.of(
                new RepasseRow("pendente", bd("90.00")),
                new RepasseRow("processado", bd("50.00")));

        var r = RelatorioService.agregarFinanceiro(pags, reps, null, null);

        assertEquals(2, r.getTotalPagamentos());
        assertEquals(bd("300.00"), r.getTotalBruto());
        assertEquals(bd("30.00"), r.getTotalTaxa());
        assertEquals(bd("270.00"), r.getTotalLiquido());
        assertEquals(bd("135.00"), r.getTicketMedio());
        assertEquals(1L, r.getPorMetodo().get("pix"));
        assertEquals(1L, r.getPorMetodo().get("cartao"));
        assertEquals(0L, r.getPorMetodo().get("boleto"));
        assertEquals(1L, r.getPorStatus().get("aprovado"));
        assertEquals(1L, r.getRepassesPendentes());
        assertEquals(1L, r.getRepassesProcessados());
        assertEquals(bd("90.00"), r.getValorRepassesPendentes());
    }

    @Test
    void financeiroVazioZeraTudo() {
        var r = RelatorioService.agregarFinanceiro(List.of(), List.of(), null, null);
        assertEquals(0, r.getTotalPagamentos());
        assertEquals(bd("0.00"), r.getTicketMedio());
        assertEquals(0L, r.getPorStatus().get("pendente"));
    }

    @Test
    void usuariosAgrega() {
        var r = RelatorioService.agregarUsuarios(
                List.of("ativo", "ativo", "suspenso"),
                List.of("cliente", "cliente", "profissional"),
                2,
                List.of("pendente", "aprovado"));

        assertEquals(3, r.getTotal());
        assertEquals(2, r.getAtivos());
        assertEquals(2, r.getNovosNoPeriodo());
        assertEquals(2L, r.getPorPerfil().get("cliente"));
        assertEquals(1L, r.getPorStatus().get("suspenso"));
        assertEquals(1L, r.getCuidadoresPorVerificacao().get("pendente"));
        assertEquals(0L, r.getCuidadoresPorVerificacao().get("correcao"));
    }

    @Test
    void contratosAgrega() {
        var r = RelatorioService.agregarContratos(List.of(
                new ContratoRow("ativo", bd("50.00")),
                new ContratoRow("ativo", bd("70.00")),
                new ContratoRow("em_disputa", bd("60.00"))), 3);

        assertEquals(3, r.getTotal());
        assertEquals(2L, r.getPorStatus().get("ativo"));
        assertEquals(1L, r.getPorStatus().get("em_disputa"));
        assertEquals(0L, r.getPorStatus().get("cancelado"));
        assertEquals(bd("60.00"), r.getValorMedioHora());
    }

    @Test
    void avaliacoesAgregaEDistribui() {
        var r = RelatorioService.agregarAvaliacoes(List.of(5, 5, 4, 3));

        assertEquals(4, r.getTotal());
        assertEquals(bd("4.25"), r.getMediaGeral());
        assertEquals(2L, r.getDistribuicao().get("5"));
        assertEquals(0L, r.getDistribuicao().get("1"));
    }

    @Test
    void denunciasAgrega() {
        var r = RelatorioService.agregarDenuncias(List.of("aberta", "em_analise", "resolvida", "aberta"));

        assertEquals(4, r.getTotal());
        assertEquals(3, r.getAbertas());
        assertEquals(2L, r.getPorStatus().get("aberta"));
    }
}
