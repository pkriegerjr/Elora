package com.elora.module.notificacao.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mensagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMensagem;
    private String conteudo;
    private LocalDate dataEnvio;
    private String statusMensagem;
    @ManyToOne private com.elora.module.usuario.entity.Usuario remetente;
    @ManyToOne private com.elora.module.usuario.entity.Usuario destinatario;
    public void enviarMensagem(){}
    public void excluirMensagem(){}
}
