package com.elora.module.usuario.entity;

import com.elora.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "usuario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;
    private String nome;
    @Column(unique = true) private String email;
    private String senha;
    private String telefone;
    private String status; // ATIVO, INATIVO, PENDENTE
    private LocalDate dataCadastro;
}
