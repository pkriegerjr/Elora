package com.elora.module.escala.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Disponibilidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDisponibilidade;
    private String diaSemana;
    private String horarioInicio;
    private String horarioFim;
    private String statusDisponibilidade;
    @ManyToOne private com.elora.module.profissional.entity.Profissional profissional;
    public void cadastrarDisponibilidade(){}
    public void removerDisponibilidade(){}
}
