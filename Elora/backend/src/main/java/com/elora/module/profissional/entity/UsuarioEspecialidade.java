package com.elora.module.profissional.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="usuario_especialidade")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(UsuarioEspecialidadeId.class)
public class UsuarioEspecialidade {
    @Id @Column(name="usuario_id") private Integer usuarioId;
    @Id @Column(name="especialidade_id") private Integer especialidadeId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="especialidade_id", insertable=false, updatable=false)
    private Especialidade especialidade;
}
@Data @NoArgsConstructor @AllArgsConstructor
class UsuarioEspecialidadeId implements java.io.Serializable { 
    private Integer usuarioId; private Integer especialidadeId; 
}