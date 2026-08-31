package com.elora.modules.usuarios.entity;

import java.math.BigDecimal;

public class UsuarioExterno {
    public Integer id;
    public String nome;
    public String email;
    public String telefone;
    public String fotoUrl;
    public String senhaHash;
    public String origemLogin;
    public String googleId;
    public String tipoPerfil; // contratante | contratado
    public String status;
    public Boolean emailVerificado;
    public BigDecimal latitude;
    public BigDecimal longitude;
    public String criadoEm;

    // contratante_detalhes
    public String cpf;
    public String observacoesCuidado;

    // contratado_detalhes
    public String especialidade;
    public String descricaoPerfil;
    public BigDecimal precoHora;
    public Boolean documentoVerificado;
    public BigDecimal notaMedia;
}
