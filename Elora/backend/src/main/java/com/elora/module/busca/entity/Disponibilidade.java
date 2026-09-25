package com.elora.module.busca.entity;

import com.elora.module.busca.enums.Periodo;
import com.elora.module.busca.enums.StatusDisponibilidade;
import com.elora.module.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MÓDULO BUSCA - Agenda do cuidador (tabela {@code disponibilidade}).
 * Filtro de data+período da busca. UNIQUE(usuario_id,data,periodo) no banco.
 */
@Entity
@Table(name = "disponibilidade", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "data", "periodo"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Disponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidade")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Periodo periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusDisponibilidade status = StatusDisponibilidade.disponivel;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
