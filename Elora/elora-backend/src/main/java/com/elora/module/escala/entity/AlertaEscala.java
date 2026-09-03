package com.elora.module.escala.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertaEscala {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAlerta;
    private String mensagem;
    private LocalDate dataEnvio;
    private String statusAlerta;
    @ManyToOne private EscalaTrabalho escala;
    public void gerarAlerta(){}
    public void enviarAlerta(){}
}
