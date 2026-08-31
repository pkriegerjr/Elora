package com.elora.modules.usuarios.dto;

public class CreateUsuarioInternoRequest {
    public String nome;
    public String email;
    public String senha; // texto puro, vai virar senha_hash com BCrypt
    public String origemLogin; // senha | google
    public String googleId;
    public String tipoPerfil; // admin | moderador | juridico
    public String regiao; // se moderador
    public String oab; // se juridico
}
