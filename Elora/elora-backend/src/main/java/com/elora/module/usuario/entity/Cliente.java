package com.elora.module.usuario.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "cliente")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCliente;
    @OneToOne @JoinColumn(name = "usuario_id") private Usuario usuario;
    private String cpf;
    private String endereco;
    private LocalDate dataNascimento;
    public void atualizarEndereco(String novo){ this.endereco = novo; }
    public void solicitarContratacao(){ /* regra de negócio */ }
}
