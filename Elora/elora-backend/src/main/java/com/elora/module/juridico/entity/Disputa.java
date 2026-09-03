package com.elora.module.juridico.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Disputa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDisputa;
    private String descricao;
    private String statusDisputa;
    private LocalDate dataAbertura;
    @ManyToOne private com.elora.module.contrato.entity.Contrato contrato;
    public void abrirDisputa(){}
    public void encerrarDisputa(){}
    public void atualizarStatus(){}
}
