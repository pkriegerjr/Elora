package com.elora.module.relatorio.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@MappedSuperclass
@Getter @Setter
public abstract class Relatorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRelatorio;
    private String titulo;
    private LocalDate periodoInicial;
    private LocalDate periodoFinal;
    private LocalDate dataGeracao;
    public abstract void gerar();
    public void exportarPDF(){}
    public void exportarExcel(){}
}
