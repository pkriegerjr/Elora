package com.elora.module.contrato.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssinaturaDigital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAssinatura;
    private LocalDate dataAssinatura;
    private String hashAssinatura;
    private String statusAssinatura;
    @OneToOne private Contrato contrato;
    public void solicitarAssinatura(){}
    public void confirmarAssinatura(){}
    public void validarAssinatura(){}
}
