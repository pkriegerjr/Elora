import javax.annotation.processing.Generated;

import jakarta.persistence.*;

@Entity @Table (name = "analise_juridica")
public class analisejuridica  { @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
private Integer idAnalise;
private Integer contratoId;
private String parecer,statusanalise="pendente"; 
}