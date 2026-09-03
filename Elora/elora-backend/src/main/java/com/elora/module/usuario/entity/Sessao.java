package com.elora.module.usuario.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sessao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sessao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSessao;
    private String token;
    private LocalDateTime dataInicio;
    private LocalDateTime dataExpiracao;
    @ManyToOne private Usuario usuario;
    public void iniciarSessao(){ this.dataInicio = LocalDateTime.now(); }
    public void encerrarSessao(){ this.dataExpiracao = LocalDateTime.now(); }
    public boolean validarSessao(){ return dataExpiracao.isAfter(LocalDateTime.now()); }
}
