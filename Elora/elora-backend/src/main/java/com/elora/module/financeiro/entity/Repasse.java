package com.elora.module.financeiro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Repasse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRepasse;
    private Double valorRepasse;
    private LocalDate dataRepasse;
    private String statusRepasse;
    @ManyToOne private Pagamento pagamento;
    @ManyToOne private com.elora.module.profissional.entity.Profissional profissional;
    public Double calcularRepasse(){ return valorRepasse * 0.85; }
    public void realizarRepasse(){}
    public void cancelarRepasse(){}
}
