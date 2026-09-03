package com.elora.module.avaliacao.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Denuncia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDenuncia;
    private String motivo;
    private String descricao;
    private LocalDate dataDenuncia;
    private String statusDenuncia;
    public void registrarDenuncia(){}
    public void atualizarStatus(){}
    public void encaminharJuridico(){}
}
