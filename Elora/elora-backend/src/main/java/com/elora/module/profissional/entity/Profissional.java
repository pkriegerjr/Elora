package com.elora.module.profissional.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "profissional")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profissional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProfissional;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private String endereco;
    private String statusValidacao;
    private String statusAtividade;
    private LocalDate dataCadastro;
    public void cadastrar(){}
    public void atualizarPerfil(){}
    public void enviarDocumentos(){}
    public void alterarStatusAtividade(String status){ this.statusAtividade = status; }
}
