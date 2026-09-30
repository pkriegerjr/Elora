package com.elora.module.busca.enums;

import com.elora.common.exception.BusinessException;

/**
 * MÓDULO BUSCA - Critérios quantitativos ordenáveis (extensível).
 *
 * <p>Para adicionar um critério novo (ex.: distancia): crie o valor aqui e o
 * comparador correspondente no service. Valor inválido na URL → 422 com
 * mensagem clara (parse case-insensitive para o front não quebrar à toa).</p>
 */
public enum OrdenacaoBusca {

    preco("valor por hora"),
    nota("nota média"),
    nome("nome");

    private final String descricao;

    OrdenacaoBusca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Converte o texto da URL (aceita maiúsculas) ou barra com 422. */
    public static OrdenacaoBusca parse(String texto) {
        if (texto == null) {
            return nota; // padrão: melhores avaliados primeiro
        }
        for (OrdenacaoBusca o : values()) {
            if (o.name().equalsIgnoreCase(texto.trim())) {
                return o;
            }
        }
        throw new BusinessException("ordenacao inválida (use preco, nota ou nome)");
    }
}
