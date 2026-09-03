package com.elora.module.escala.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Turno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTurno;
    private LocalTime horarioInicio;
    private LocalTime horarioFim;
    private String tipoTurno;
    @ManyToOne private EscalaTrabalho escala;
    public void definirTurno(){}
    public void alterarTurno(){}
}
