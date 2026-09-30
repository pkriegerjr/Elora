package com.elora.module.profissional.dto;
import lombok.Data; import java.math.BigDecimal; import java.util.List;
@Data public class ProfissionalResponse {
  Integer id; String nome; String email; String cpf; String telefone; String fotoUrl;
  String genero; BigDecimal precoHora; String descricaoPerfil; String statusVerificacao;
  Boolean documentoVerificado; BigDecimal notaMedia; List<String> especialidades;
  List<DocumentoDTO> documentos;
  @Data public static class DocumentoDTO { Integer id; String tipo; String arquivoUrl; String status; }
}