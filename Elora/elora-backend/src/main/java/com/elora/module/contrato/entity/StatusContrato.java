package com.elora.module.contrato.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatusContrato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idStatus;
    private String nomeStatus;
    private String descricao;
    public void atualizarStatus(){}
}
