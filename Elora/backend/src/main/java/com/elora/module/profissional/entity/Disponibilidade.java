package com.elora.module.profissional.entity;
import com.elora.module.profissional.enums.Periodo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity(name = "ProfissionalDisponibilidade") @Table(name="disponibilidade", uniqueConstraints=@UniqueConstraint(columnNames={"usuario_id","data","periodo"})) @Data
public class Disponibilidade {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_disponibilidade") private Integer id;
  @Column(name="usuario_id",nullable=false) private Integer usuarioId;
  @Column(nullable=false) private LocalDate data;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Periodo periodo;
  @Column(nullable=false) private String status="disponivel";
  @CreationTimestamp @Column(name="criado_em",nullable=false,updatable=false) private LocalDateTime criadoEm;
}