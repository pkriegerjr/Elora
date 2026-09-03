package com.elora.module.usuario.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity @Table(name = "perfil_acesso")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerfilAcesso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPerfil;
    private String nomePerfil;
    private String descricao;
    @ManyToMany
    @JoinTable(name = "perfil_permissao", joinColumns = @JoinColumn(name = "perfil_id"), inverseJoinColumns = @JoinColumn(name = "permissao_id"))
    private Set<Permissao> permissoes;
    public void definirPermissao(Permissao p){ permissoes.add(p); }
    public void removerPermissao(Permissao p){ permissoes.remove(p); }
}
