package com.elora.module.financeiro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pagamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPagamento;
    private Double valor;
    private LocalDate dataPagamento;
    private String formaPagamento;
    private String statusPagamento;
    @ManyToOne private com.elora.module.contrato.entity.Contrato contrato;
    public void registrarPagamento(){}
    public void confirmarPagamento(){}
    public void cancelarPagamento(){}
}
