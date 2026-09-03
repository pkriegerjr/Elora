package com.elora.module.usuario.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "operador")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Operador {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOperador;
    @OneToOne @JoinColumn(name = "usuario_id") private Usuario usuario;
    private String setor; // ADMINISTRADOR, SUPORTE, FINANCEIRO, JURIDICO
    private String cargo;
    public void validarCadastro(){}
    public void consultarUsuario(){}
}
