package com.elora.module.usuario.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PATCH /clients/me — atualização do próprio contratante.
 * Campos nulos não são alterados. {@code observacoesCuidado} vive em
 * {@code contratante_detalhes}; o restante em {@code usuario}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateRequest {

    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    private String nome;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    private String observacoesCuidado;

    private Double latitude;

    private Double longitude;
}
