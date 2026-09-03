package com.elora.module.busca.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "busca_profissional")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BuscaProfissional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idBusca;
    private LocalDate dataBusca;
    private String termoBusca;
    public void buscarPorFiltros(){}
    public void buscarPorLocalizacao(){}
    public void listarResultados(){}
}
