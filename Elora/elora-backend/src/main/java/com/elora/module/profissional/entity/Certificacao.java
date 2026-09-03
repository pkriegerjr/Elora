package com.elora.module.profissional.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCertificacao;
    private String nomeCertificacao;
    private String instituicao;
    private LocalDate dataConclusao;
    private String arquivoComprovante;
    @ManyToOne private Profissional profissional;
    public void registrarCertificacao(){}
    public void validarCertificacao(){}
}
