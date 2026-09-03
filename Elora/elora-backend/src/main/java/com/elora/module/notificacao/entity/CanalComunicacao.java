package com.elora.module.notificacao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanalComunicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCanal;
    private String tipoCanal; // EMAIL, SMS, PUSH
    private Boolean ativo;
    public void ativarCanal(){ this.ativo = true; }
    public void desativarCanal(){ this.ativo = false; }
}
