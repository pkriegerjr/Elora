package com.elora.module.profissional.dto;
import jakarta.validation.constraints.*; import lombok.Data; import java.math.BigDecimal; import java.util.List;
@Data public class ProfissionalUpdateRequest {
  @Size(max=150) String nome; @Size(max=20) String telefone;
  String descricaoPerfil;
  @DecimalMin("0.0") BigDecimal precoHora;
  List<String> especialidades; // nomes: idoso, alzheimer...
}