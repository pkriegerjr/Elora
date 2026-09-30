package com.elora.module.profissional.entity;
import lombok.*;
import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
public class UsuarioEspecialidadeId implements Serializable {
    private Integer usuarioId;
    private Integer especialidadeId;
}