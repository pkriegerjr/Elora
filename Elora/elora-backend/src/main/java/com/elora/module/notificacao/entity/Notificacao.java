package com.elora.module.notificacao.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacao;
    private String titulo;
    private String mensagem;
    private String tipoNotificacao;
    private LocalDate dataEnvio;
    private String statusLeitura;
    @ManyToOne private com.elora.module.usuario.entity.Usuario usuario;
    @ManyToOne private CanalComunicacao canal;
    public void criarNotificacao(){}
    public void enviarNotificacao(){}
    public void marcarComoLida(){}
}
