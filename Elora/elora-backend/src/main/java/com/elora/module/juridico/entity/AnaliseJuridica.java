package com.elora.module.juridico.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnaliseJuridica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAnalise;
    private String parecer;
    private LocalDate dataAnalise;
    private String statusAnalise;
    @ManyToOne private com.elora.module.contrato.entity.Contrato contrato;
    public void registrarParecer(){}
    public void aprovarAnalise(){}
    public void reprovarAnalise(){}
}
