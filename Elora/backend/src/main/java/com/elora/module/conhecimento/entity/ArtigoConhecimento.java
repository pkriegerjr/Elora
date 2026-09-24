import java.time.LocalDate;

import javax.annotation.processing.Generated;

@Entity @Table (name="artigo_conhecimento")
@Getter @Setter @NoArgsConstructor @Builder
public class ArtigoConhecimento extends BaseEntity {
    @Id @GeneratedValue(IDENTITY) @Column(name="id_artigo") Long idArtigo;
    @Column (nullable=false) String titulo;
    @Column (nullable=false, columnDefinition="TEXT") String Conteudo;
    @Column (length=100) String categoria;
    @Column (name="data_publicacao") LocalDate dataPublicaco;
    @ManyToOne(fetch=LAZY) @JoinColumn(name="categoria_id") CategoriaConteudo CategoriaConteudo; 
}