package com.elora.module.profissional.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class ValidacaoRequest {
  @NotBlank String resultado; // aprovado | reprovado | correcao
  String observacao;
}