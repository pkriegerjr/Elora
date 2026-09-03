package com.elora.module.profissional.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "documento_profissional")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentoProfissional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDocumento;
    private String tipoDocumento;
    private String numeroDocumento;
    private String arquivo; // S3 key
    private String statusDocumento;
    private LocalDate dataEnvio;
    @ManyToOne private Profissional profissional;
    public void anexarDocumento(){}
    public void validarDocumento(){}
    public void reprovarDocumento(){}
}
