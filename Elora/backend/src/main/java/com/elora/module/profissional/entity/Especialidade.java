package com.elora.module.profissional.entity;
import jakarta.persistence.*; import lombok.Data;
@Entity @Table(name="especialidade") @Data
public class Especialidade { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_especialidade") private Integer id; @Column(unique=true,nullable=false) private String nome; }