package com.elora.module.juridico.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessoRescisao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRescisao;
    private String motivo;
    private LocalDate dataSolicitacao;
    private String statusRescisao;
    @ManyToOne private com.elora.module.contrato.entity.Contrato contrato;
    public void solicitarRescisao(){}
    public void aprovarRescisao(){}
    public void negarRescisao(){}
}
