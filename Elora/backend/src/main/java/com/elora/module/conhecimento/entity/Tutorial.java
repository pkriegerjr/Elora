@Entity @Table(name="tutorial")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tutorial extends BaseEntity {
    @Id @GeneratedValue(IDENTITY) @Column(name="id_tutorial") Long idTutorial;
    @Column(nullable=false) String Titulo;
    @Column(columnDefinition="TEXT") String descricao;
    @Column(name="link_conteudo", length=500) String LinkConteudo;
    @Column(length=50) String status;

    @ManyToOne(fetch=LAZY) @JoinColumn(name="categoria_id") CategoriaConteudo CategoriaConteudo;

}