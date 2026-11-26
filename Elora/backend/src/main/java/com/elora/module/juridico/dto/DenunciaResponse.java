package com.elora.module.juridico.dto;

import com.elora.module.juridico.enums.StatusDenuncia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** MÓDULO JURÍDICO - Cartão/detalhe da denúncia devolvido ao front. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DenunciaResponse {

    private Integer id;
    private Integer denuncianteId;
    private Integer denunciadoId;
    private Integer contratoId;
    private String motivo;
    private String descricao;
    private String parecer;
    private StatusDenuncia status;
    private Integer analisadoPor;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
