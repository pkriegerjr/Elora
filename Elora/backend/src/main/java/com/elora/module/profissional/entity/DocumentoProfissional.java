package com.elora.module.profissional.entity;
import jakarta.persistence.*;
import lombok.Data; import java.time.LocalDateTime;
@Entity @Table(name="documento_profissional") @Data
public class DocumentoProfissional {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_documento") private Integer id;
  @Column(name="usuario_id", nullable=false) private Integer usuarioId;
  @Column(nullable=false,length=80) private String tipo; // RG, CPF, certificado, referencia
  @Column(name="arquivo_url",nullable=false,length=500) private String arquivoUrl;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private com.elora.module.profissional.enums.DocumentoStatus status = com.elora.module.profissional.enums.DocumentoStatus.pendente;
  @Column(name="verificado_por") private Integer verificadoPor;
  @Column(name="verificado_em") private LocalDateTime verificadoEm;
  @Column(name="criado_em", updatable=false) private LocalDateTime criadoEm = LocalDateTime.now();
}