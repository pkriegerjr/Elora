package com.elora.module.busca.enums;

/**
 * MÓDULO BUSCA - Enum da situação de um turno na agenda.
 *
 * <p>Espelha o CHECK da coluna {@code disponibilidade.status}: o turno pode
 * estar livre (disponivel), já ocupado por um serviço (ocupado) ou
 * bloqueado pelo cuidador (indisponivel). A busca só mostra quem está
 * {@code disponivel} no dia/período pedido.</p>
 */
public enum StatusDisponibilidade {

    // Turno livre: aparece na busca.
    disponivel,

    // Turno já ocupado por outro serviço: não aparece na busca.
    ocupado,

    // Turno bloqueado pelo cuidador (folga, férias...): não aparece na busca.
    indisponivel
}
