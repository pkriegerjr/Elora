package com.elora.module.juridico.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PainelJuridico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPainel;
    private LocalDate dataAtualizacao;
    public void listarContratosEmAnalise(){}
    public void listarDenuncias(){}
    public void listarRescisoes(){}
}
