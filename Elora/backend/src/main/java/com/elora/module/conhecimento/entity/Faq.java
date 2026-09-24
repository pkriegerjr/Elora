@Entity @Table (name="faq")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Faq extends BaseEntity{
    @Id @GeneratedValue(IDENTITY) @Column(name="id_faq") Long idFaq;
    @Column(nullable=false, columnDefinition="TEXT") String pergunta;
    @Column(nullable=false, ColumnDefinition="TEXT") String resposta;
    @Column(length=100) String categoria;
    @Column(length=50) String status;

    @ManyToOne(fetch=LAZY) @JoinColumn(name="categoria_id") CategoriaConteudo CategoriaConteudo;
    public void cadastrarPergunta(){this.status="RASCUNHO";}
    public void atualizarResposta(String r){this.resposta=r;}
    public void publicarFaq(){this.status="PUBLICADO";}
}