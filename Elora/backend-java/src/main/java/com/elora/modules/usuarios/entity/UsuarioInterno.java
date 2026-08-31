package com.elora.modules.usuarios.entity;

public class UsuarioInterno {
    public Integer id;
    public String nome;
    public String email;
    public String senhaHash;
    public String origemLogin; // senha | google
    public String googleId;
    public String tipoPerfil; // admin | moderador | juridico
    public String status;
    public Boolean emailVerificado;
    public String criadoEm;
    public String atualizadoEm;

    // Detalhes específicos
    public String regiao; // moderador_regiao
    public String oab; // juridico_detalhes
    public Boolean permissaoEditarContrato;
    public Boolean permissaoAprovarTermo;
}
