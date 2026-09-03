package com.elora.module.contrato.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contrato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idContrato;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Double valorTotal;
    private String statusContrato;
    @OneToOne private PropostaContrato proposta;
    @ManyToOne private StatusContrato status;
    public void gerarContrato(){}
    public void ativarContrato(){}
    public void encerrarContrato(){}
    public void cancelarContrato(){}
}
