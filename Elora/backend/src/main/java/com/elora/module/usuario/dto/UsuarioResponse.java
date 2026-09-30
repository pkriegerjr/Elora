package com.elora.module.usuario.dto;

import com.elora.module.usuario.enums.Genero;
import com.elora.module.usuario.enums.UsuarioStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Usuário exposto à API. Sem senha/hash. Retornado diretamente (sem envelope),
 * pois o front lê {@code res.user} / {@code res.accessToken}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Integer id;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    private String fotoUrl;
    private Genero genero;
    private LocalDate dataNascimento;
    private UsuarioStatus status;
    private List<String> perfis;
    private Boolean emailVerificado;

    /**
     * Situação do cadastro do cuidador (PENDING/APPROVED, via documento_verificado).
     * Nulo para não-profissionais. O front usa para o gate de login do cuidador
     * quando MOCK_MODE=false (ver authService.loginCuidador).
     */
    private String situacaoCadastro;
    private Boolean documentoVerificado;
    private BigDecimal precoHora;
    private BigDecimal notaMedia;

    private LocalDateTime criadoEm;
}
