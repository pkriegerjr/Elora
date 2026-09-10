import javax.annotation.processing.Generated;

import jakarta.persistence.*;

@Entity @Table (name = "processo_rescisao") public class processoRescisao {@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
private Integer idAnalise;
private Integer contratoId;
private String parecer,statusAnalise="solicitado";
}