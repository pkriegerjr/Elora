package com.elora.module.avaliacao.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Avaliacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAvaliacao;
    private Integer nota;
    private String comentario;
    private LocalDate dataAvaliacao;
    @ManyToOne private com.elora.module.profissional.entity.Profissional profissional;
    @ManyToOne private com.elora.module.usuario.entity.Cliente cliente;
    public void registrarAvaliacao(){}
    public void editarAvaliacao(){}
    public void removerAvaliacao(){}
}
