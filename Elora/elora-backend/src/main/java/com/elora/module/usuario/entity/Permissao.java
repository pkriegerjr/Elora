package com.elora.module.usuario.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "permissao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPermissao;
    private String nomePermissao;
    private String descricao;
    public boolean validarPermissao(){ return true; }
}
