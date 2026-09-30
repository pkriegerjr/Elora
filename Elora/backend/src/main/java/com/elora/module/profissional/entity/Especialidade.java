package com.elora.module.profissional.entity;
import jakarta.persistence.*; import lombok.*;
@Entity(name = "ProfissionalEspecialidade") @Table(name="especialidade") @Data @NoArgsConstructor @AllArgsConstructor
public class Especialidade { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_especialidade") private Integer id; @Column(unique=true,nullable=false) private String nome; }