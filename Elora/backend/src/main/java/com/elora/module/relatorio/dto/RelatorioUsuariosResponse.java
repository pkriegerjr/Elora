package com.elora.module.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** GET /relatorios/usuarios — base, perfis, atividade e fila de validação. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioUsuariosResponse {

    private long total;
    private long ativos;
    private long novosNoPeriodo;
    private Map<String, Long> porPerfil;
    private Map<String, Long> porStatus;
    private Map<String, Long> cuidadoresPorVerificacao;
}
