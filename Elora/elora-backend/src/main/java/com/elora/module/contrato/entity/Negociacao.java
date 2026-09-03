package com.elora.module.contrato.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Negociacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNegociacao;
    private String mensagem;
    private LocalDate dataMensagem;
    private Double valorSugerido;
    @ManyToOne private Contrato contrato;
    public void registrarMensagem(){}
    public void alterarValor(){}
}
