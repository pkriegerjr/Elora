package com.elora.module.busca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * MÓDULO BUSCA - Favorito (tabela {@code favorito}, REQ-ELO-018).
 * PK composta impede favoritar 2x (além da checagem amigável no service).
 */
@Entity
@Table(name = "favorito")
@IdClass(FavoritoId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorito {

    @Id
    @Column(name = "cliente_id")
    private Integer clienteId;

    @Id
    @Column(name = "profissional_id")
    private Integer profissionalId;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
