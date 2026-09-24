@Entity @Table (name="categoria_conteudo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaConteudo extends BaseEntity {
    @Id @GeneratedValue(IDENTITY) @Column(name="id_categoria") Long idCategoria;
    @Column (name="nome_categoria", nullable=false, unique=true) String nomeCategoria;
    @Column(columnDefinition="TEXT") String descricao;
    public void cadastrarCategoria(String n, String d){this.nomeCategoria=n; this.descricao=d;}
    public void atualizarCategoria(String n, Strind d){if(n!=null) nomeCategoria=n; if(d!=null) descricao=d;}
}