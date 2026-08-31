package com.elora.modules.usuarios.repository;

import com.elora.config.DatabaseConfig;
import com.elora.modules.usuarios.entity.UsuarioExterno;
import com.elora.modules.usuarios.entity.UsuarioInterno;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository {

    public List<UsuarioInterno> findAllInternos() throws SQLException {
        List<UsuarioInterno> list = new ArrayList<>();
        String sql = "SELECT u.*, m.regiao, j.oab, j.permissao_editar_contrato, j.permissao_aprovar_termo " +
                     "FROM usuario_interno u " +
                     "LEFT JOIN moderador_regiao m ON m.id_usuario_interno = u.id_usuario_interno " +
                     "LEFT JOIN juridico_detalhes j ON j.id_usuario_interno = u.id_usuario_interno";
        try (Connection c = DatabaseConfig.getDataSource().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                UsuarioInterno u = new UsuarioInterno();
                u.id = rs.getInt("id_usuario_interno");
                u.nome = rs.getString("nome");
                u.email = rs.getString("email");
                u.origemLogin = rs.getString("origem_login");
                u.tipoPerfil = rs.getString("tipo_perfil");
                u.status = rs.getString("status");
                u.emailVerificado = rs.getBoolean("email_verificado");
                u.regiao = rs.getString("regiao");
                u.oab = rs.getString("oab");
                list.add(u);
            }
        }
        return list;
    }

    public List<UsuarioExterno> findAllExternos() throws SQLException {
        List<UsuarioExterno> list = new ArrayList<>();
        String sql = "SELECT u.*, " +
                     "c.cpf as c_cpf, c.observacoes_cuidado, " +
                     "t.cpf as t_cpf, t.especialidade, t.descricao_perfil, t.preco_hora, t.documento_verificado, t.nota_media " +
                     "FROM usuario_externo u " +
                     "LEFT JOIN contratante_detalhes c ON c.id_usuario_externo = u.id_usuario_externo " +
                     "LEFT JOIN contratado_detalhes t ON t.id_usuario_externo = u.id_usuario_externo";
        try (Connection c = DatabaseConfig.getDataSource().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                UsuarioExterno u = new UsuarioExterno();
                u.id = rs.getInt("id_usuario_externo");
                u.nome = rs.getString("nome");
                u.email = rs.getString("email");
                u.telefone = rs.getString("telefone");
                u.tipoPerfil = rs.getString("tipo_perfil");
                u.status = rs.getString("status");
                u.latitude = rs.getBigDecimal("latitude");
                u.longitude = rs.getBigDecimal("longitude");
                // pega cpf correto conforme tipo
                u.cpf = rs.getString("c_cpf") != null ? rs.getString("c_cpf") : rs.getString("t_cpf");
                u.especialidade = rs.getString("especialidade");
                u.precoHora = rs.getBigDecimal("preco_hora");
                u.notaMedia = rs.getBigDecimal("nota_media");
                list.add(u);
            }
        }
        return list;
    }

    public int insertInterno(String nome, String email, String senhaHash, String origem, String tipo) throws SQLException {
        String sql = "INSERT INTO usuario_interno (nome, email, senha_hash, origem_login, tipo_perfil) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.setString(2, email);
            ps.setString(3, senhaHash);
            ps.setString(4, origem);
            ps.setString(5, tipo);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public int insertExterno(String nome, String email, String senhaHash, String origem, String tipo, String telefone) throws SQLException {
        String sql = "INSERT INTO usuario_externo (nome, email, senha_hash, origem_login, tipo_perfil, telefone) VALUES (?,?,?,?,?,?)";
        try (Connection c = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.setString(2, email);
            ps.setString(3, senhaHash);
            ps.setString(4, origem);
            ps.setString(5, tipo);
            ps.setString(6, telefone);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }
}
