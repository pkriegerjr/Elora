package com.elora.module.juridico.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STUB in-memory (NÃO é @Entity).
 *
 * Motivo: o schema oficial v2 (elora_schema_v2.sql) NÃO tem as tabelas
 * `analise_juridica` / `processo_rescisao`. O jurídico em v2 vive em
 * `denuncia` / `evidencia_denuncia` / `disputa` + `contrato`.
 * Manter @Entity aqui quebrava o startup com ddl-auto=validate.
 *
 * TODO (dono do módulo jurídico): trocar este stub por entity mapeada
 * para denuncia/disputa ou criar migration nova — NÃO editar o v2 à mão.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnaliseJuridica {
    private Integer idAnalise;
    private Integer contratoId;
    private String parecer;
    private String statusAnalise = "pendente";
}
