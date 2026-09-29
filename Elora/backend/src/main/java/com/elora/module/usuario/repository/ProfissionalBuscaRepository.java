package com.elora.module.usuario.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Leitura da vitrine de cuidadores sobre {@code vw_profissional_busca}
 * (schema v2). Query nativa — nenhuma entidade nova, o {@code validate}
 * do Hibernate não é afetado.
 */
@Repository
@RequiredArgsConstructor
public class ProfissionalBuscaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String COLUNAS = """
            v.id_usuario, v.nome, v.foto_url, v.descricao_perfil, v.preco_hora,
            v.nota_media, v.status_verificacao, v.especialidades, v.latitude, v.longitude,
            (SELECT COUNT(*) FROM avaliacao a WHERE a.avaliado_id = v.id_usuario) AS total_avaliacoes
            """;

    private static final String FILTROS = """
            FROM vw_profissional_busca v
            WHERE (:specialty IS NULL OR EXISTS (
                SELECT 1 FROM usuario_especialidade ue
                JOIN especialidade e ON e.id_especialidade = ue.especialidade_id
                WHERE ue.usuario_id = v.id_usuario
                  AND LOWER(e.nome) LIKE '%' || LOWER(REPLACE(:specialty, '_', ' ')) || '%'))
              AND (:minRating IS NULL OR v.nota_media >= :minRating)
            """;

    /** Vitrine com filtros opcionais, ordenada por avaliação. */
    public List<Map<String, Object>> buscar(String specialty, BigDecimal minRating) {
        String sql = "SELECT " + COLUNAS + FILTROS + " ORDER BY v.nota_media DESC, v.nome ASC";
        // Tipos explícitos: o Postgres não infere o tipo de parâmetro NULL
        // ("could not determine data type of parameter $1"); no MySQL funcionava.
        var params = new MapSqlParameterSource()
                .addValue("specialty", specialty, Types.VARCHAR)
                .addValue("minRating", minRating, Types.NUMERIC);
        return jdbc.queryForList(sql, params);
    }

    /** Detalhe público de um cuidador pelo id (só ativos — a view já filtra). */
    public Optional<Map<String, Object>> buscarPorId(Integer id) {
        String sql = "SELECT " + COLUNAS + FILTROS + " AND v.id_usuario = :id";
        var params = new MapSqlParameterSource()
                .addValue("specialty", null, Types.VARCHAR)
                .addValue("minRating", null, Types.NUMERIC)
                .addValue("id", id, Types.INTEGER);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.stream().findFirst();
    }
}
