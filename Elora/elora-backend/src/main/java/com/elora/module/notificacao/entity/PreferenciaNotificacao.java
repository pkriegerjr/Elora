package com.elora.module.notificacao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PreferenciaNotificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPreferencia;
    private Boolean receberEmail;
    private Boolean receberSMS;
    private Boolean receberPush;
    @OneToOne private com.elora.module.usuario.entity.Usuario usuario;
    public void atualizarPreferencias(){}
}
