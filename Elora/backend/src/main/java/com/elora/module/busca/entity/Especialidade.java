package com.elora.module.busca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÓDULO BUSCA - Catálogo de especialidades (tabela {@code especialidade}).
 * Tags qualitativas: filtram (não ordenam) — quem tem a tag entra no resultado.
 */
@Entity
@Table(name = "especialidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Especialidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidade")
    private Integer id;

    @Column(nullable = false, unique = true, length = 150)
    private String nome;
}
