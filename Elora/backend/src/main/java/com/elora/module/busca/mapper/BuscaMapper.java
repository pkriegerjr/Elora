package com.elora.module.busca.mapper;

import com.elora.module.busca.dto.FavoritoResponse;
import com.elora.module.busca.dto.ProfissionalResponse;
import com.elora.module.busca.entity.Favorito;
import com.elora.module.usuario.entity.ProfissionalDetalhes;
import com.elora.module.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.util.List;

/**
 * MÓDULO BUSCA - Mapper ( Junta usuario + profissional_detalhes num cartão).
 * Estático, sem Spring. LAZY: chamar dentro de @Transactional.
 */
public final class BuscaMapper {

    private BuscaMapper() {
    }

    public static ProfissionalResponse toProfissionalResponse(
            Usuario usuario, ProfissionalDetalhes detalhes, List<String> especialidades) {
        BigDecimal preco = detalhes == null ? null : detalhes.getPrecoHora();
        BigDecimal nota = detalhes == null || detalhes.getNotaMedia() == null
                ? new BigDecimal("0.00")
                : detalhes.getNotaMedia();
        return new ProfissionalResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getFotoUrl(),
                detalhes == null ? null : detalhes.getDescricaoPerfil(),
                preco,
                nota,
                especialidades
        );
    }

    public static FavoritoResponse toFavoritoResponse(Favorito favorito) {
        return new FavoritoResponse(
                favorito.getClienteId(),
                favorito.getProfissionalId(),
                favorito.getCriadoEm()
        );
    }
}
