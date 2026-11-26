package com.elora.module.juridico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÓDULO JURÍDICO - DTO do POST /juridico/denuncias.
 *
 * <p>Quem denuncia vem do JWT, nunca do corpo — isso impede que alguém
 * registre uma denúncia em nome de outro usuário.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarDenunciaRequest {

    /** Usuário denunciado (perfil público do cuidador, por exemplo). */
    @NotNull(message = "denunciadoId é obrigatório")
    private Integer denunciadoId;

    /** Opcional: contrato relacionado ao ocorrido. */
    private Integer contratoId;

    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 150, message = "Motivo deve ter no máximo 150 caracteres")
    private String motivo;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 10, max = 5000, message = "Descrição deve ter entre 10 e 5000 caracteres")
    private String descricao;
}
