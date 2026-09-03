package com.elora.module.contrato.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropostaContrato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProposta;
    private String descricaoServico;
    private Double valorProposta;
    private LocalDate dataProposta;
    private String statusProposta;
    @ManyToOne private com.elora.module.profissional.entity.Profissional profissional;
    @ManyToOne private com.elora.module.usuario.entity.Cliente cliente;
    public void criarProposta(){}
    public void aceitarProposta(){}
    public void alterarProposta(){}
    public void recusarProposta(){}
}
