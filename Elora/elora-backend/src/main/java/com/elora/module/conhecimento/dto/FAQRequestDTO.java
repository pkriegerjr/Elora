package com.elora.module.conhecimento.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FAQRequestDTO {
    @NotBlank private String nome;
}
