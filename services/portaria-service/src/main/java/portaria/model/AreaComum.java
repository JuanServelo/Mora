package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "areas_comuns", uniqueConstraints = {
    @UniqueConstraint(name = "areas_comuns_nome_condominioId_key", columnNames = {"nome", "condominioId"})
})
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome da área comum é obrigatório")
    private String nome;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotBlank(message = "Tipo é obrigatório")
    private String tipo;

    private String descricao;

    private String localizacao;

    @Column(name = "`capacidadeMaxima`")
    private Integer capacidadeMaxima;

    private Double area;

    @Column(name = "`podeReservar`")
    private boolean podeReservar = false;

    private Double taxaLocacao;

    private String informacoesLimpeza;

    private String politicaCancelamento;

    private String observacoes;

    private boolean ativo = true;

    @Column(name = "`criadoEm`")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "`atualizadoEm`")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
