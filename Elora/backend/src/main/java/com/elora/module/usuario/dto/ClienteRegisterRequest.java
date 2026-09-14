package com.elora.module.usuario.dto;

import com.elora.module.usuario.enums.Genero;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * POST /clients. {@code ignoreUnknown=true} porque o front manda o objeto
 * completo do formulário (campos a mais não podem gerar 400).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClienteRegisterRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150)
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 150)
    private String email;

    /** Com ou sem máscara — normalizado para 11 dígitos no service. */
    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @Size(max = 20)
    private String telefone;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{8,}$",
            message = "Senha deve ter maiúscula, minúscula, número e caractere especial")
    private String senha;

    private Genero genero;

    @Past(message = "Data de nascimento deve estar no passado")
    private LocalDate dataNascimento;

    @DecimalMin(value = "-90.0", message = "Latitude inválida")
    @DecimalMax(value = "90.0", message = "Latitude inválida")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude inválida")
    @DecimalMax(value = "180.0", message = "Longitude inválida")
    private Double longitude;

    private String observacoesCuidado;

    @NotNull(message = "Consentimento LGPD é obrigatório")
    @AssertTrue(message = "É necessário aceitar os termos (LGPD)")
    private Boolean consentimentoLgpd;
}
