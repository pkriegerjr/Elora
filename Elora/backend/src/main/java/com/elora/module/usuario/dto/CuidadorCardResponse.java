package com.elora.module.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Card público do cuidador para a vitrine (busca e perfil).
 * Nomes de campos em inglês para compatibilidade direta com o front
 * ({@code buscar-cuidadores.html#renderList} e {@code perfil-cuidador.html}).
 * Sem dados sensíveis (sem e-mail, CPF, telefone) — seguro para
 * exibição a qualquer usuário autenticado (LGPD).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuidadorCardResponse {

    private Integer id;
    private String name;
    private String fotoUrl;
    private String bio;
    private BigDecimal hourlyRate;
    private BigDecimal rating;
    private Long reviewCount;
    private Boolean verified;
    /** Situação do cadastro (APPROVED/PENDING/...) para o selo do perfil. */
    private String status;
    private List<String> specialties;
    private BigDecimal latitude;
    private BigDecimal longitude;
    /** Distância em km — preenchida só quando a busca informa lat/lng. */
    private Double distanceKm;
}
