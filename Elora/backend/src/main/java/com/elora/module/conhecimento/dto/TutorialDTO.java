@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class TutorialRequestDTO {
    @NotBlank String titulo; @NotBlank String descricao; @Notblank String LinkConteudo; String status; Long categoriaid;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class TutorialResponseDTO {Long idTutorial; String titulo; String descricao; String LinkConteudo; String status; Long categoriaid;}