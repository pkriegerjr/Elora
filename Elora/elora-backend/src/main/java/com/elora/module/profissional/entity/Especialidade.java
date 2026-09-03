package com.elora.module.profissional.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Especialidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEspecialidade;
    private String nomeEspecialidade;
    private String descricao;
    public void cadastrarEspecialidade(){}
    public void atualizarEspecialidade(){}
}
