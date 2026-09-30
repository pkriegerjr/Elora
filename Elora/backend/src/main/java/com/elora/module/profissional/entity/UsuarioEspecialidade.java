package com.elora.module.profissional.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity(name = "ProfissionalUsuarioEspecialidade") @Table(name="usuario_especialidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UsuarioEspecialidadeId.class)
public class UsuarioEspecialidade {
    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Id
    @Column(name = "especialidade_id")
    private Integer especialidadeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidade_id", insertable = false, updatable = false)
    private Especialidade especialidade;
}