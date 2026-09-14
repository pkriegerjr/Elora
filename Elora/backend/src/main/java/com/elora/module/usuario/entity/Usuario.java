package com.elora.module.usuario.entity;

import com.elora.module.usuario.enums.Genero;
import com.elora.module.usuario.enums.OrigemLogin;
import com.elora.module.usuario.enums.UsuarioStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espelha {@code usuario} do schema v2 (sem alterar o banco).
 *
 * Observação: {@code geo_ponto} é coluna gerada no MySQL e NÃO é mapeada —
 * INSERT/SELECT a ignoram e o {@code validate} passa. O módulo busca fará
 * consultas de proximidade por query nativa quando precisar.
 */
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    /** Somente dígitos (CHAR(11) no banco). */
    @Column(length = 11, unique = true)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Genero genero;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    /** Hash bcrypt/argon2 — nunca texto puro. NULL quando login só via Google. */
    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_login", nullable = false, length = 10)
    private OrigemLogin origemLogin = OrigemLogin.senha;

    @Column(name = "google_id", unique = true, length = 100)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UsuarioStatus status = UsuarioStatus.ativo;

    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "consentimento_lgpd", nullable = false)
    private Boolean consentimentoLgpd = false;

    @Column(name = "consentimento_lgpd_at")
    private LocalDateTime consentimentoLgpdAt;

    /** v2.2: atualizado a cada login (relatórios/admin). */
    @Column(name = "ultimo_login_em")
    private LocalDateTime ultimoLoginEm;

    /** Soft-delete LGPD — nunca DELETE físico de titular. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
