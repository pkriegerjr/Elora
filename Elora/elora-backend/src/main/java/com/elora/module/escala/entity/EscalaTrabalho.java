package com.elora.module.escala.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EscalaTrabalho {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEscala;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String tipoEscala;
    private String statusEscala;
    @ManyToOne private com.elora.module.contrato.entity.Contrato contrato;
    public void criarEscala(){}
    public void alterarEscala(){}
    public void cancelarEscala(){}
}
