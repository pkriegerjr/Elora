package com.elora.module.profissional.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValidacaoProfissional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idValidacao;
    private LocalDate dataValidacao;
    private String resultado;
    private String observacao;
    @ManyToOne private Profissional profissional;
    public void aprovarProfissional(){}
    public void reprovarProfissional(){}
    public void solicitarCorrecao(){}
}
