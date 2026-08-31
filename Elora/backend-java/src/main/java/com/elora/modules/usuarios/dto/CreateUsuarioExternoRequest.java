package com.elora.modules.usuarios.dto;

import java.math.BigDecimal;

public class CreateUsuarioExternoRequest {
    public String nome;
    public String email;
    public String senha;
    public String telefone;
    public String origemLogin;
    public String googleId;
    public String tipoPerfil; // contratante | contratado
    public BigDecimal latitude;
    public BigDecimal longitude;

    // contratante
    public String cpf;
    public String observacoesCuidado;

    // contratado
    public String especialidade;
    public String descricaoPerfil;
    public BigDecimal precoHora;
}
