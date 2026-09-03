package com.elora.module.avaliacao.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoricoDenuncia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorico;
    private LocalDate dataRegistro;
    private String descricaoAcao;
    private String responsavel;
    @ManyToOne private Denuncia denuncia;
    public void registrarAcao(){}
}
