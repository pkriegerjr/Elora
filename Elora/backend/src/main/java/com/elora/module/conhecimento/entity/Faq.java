package com.elora.module.conhecimento.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "faq")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Faq {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_faq") private Integer id;
    @Column(nullable = false, columnDefinition = "TEXT") private String pergunta;
    @Column(nullable = false, columnDefinition = "TEXT") private String resposta;
    @Column(length = 100) private String categoria;
    @Column(length = 50) @Builder.Default private String status = "rascunho";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "categoria_id") private CategoriaConteudo categoriaRef;
    @CreationTimestamp @Column(name = "criado_em", updatable = false) private LocalDateTime criadoEm;
    public void cadastrarPergunta() { this.status = "rascunho"; }
    public void atualizarResposta(String r) { this.resposta = r; }
    public void publicarFaq() { this.status = "publicado"; }
}