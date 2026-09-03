package com.elora.module.busca.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Favorito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFavorito;
    private LocalDate dataInclusao;
    @ManyToOne private com.elora.module.usuario.entity.Cliente cliente;
    @ManyToOne private com.elora.module.profissional.entity.Profissional profissional;
    public void adicionarFavorito(){}
    public void removerFavorito(){}
}
