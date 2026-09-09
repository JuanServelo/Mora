package portaria.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movimentacoes_veiculo")
public class MovimentacaoVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String placa;

    private String modelo;

    @Column(name = "veiculo_id")
    private String veiculoId;

    /** Vaga ocupada durante esta movimentação (null para veículos de serviço). */
    @Column(name = "vaga_id")
    private String vagaId;

    @Column(name = "`condominioId`")
    private String condominioId;

    /** Snapshot do nome do proprietário/vinculado no momento da entrada. */
    @Column(name = "vinculado_nome")
    private String vinculadoNome;

    @Column(name = "registrado_por_entrada_id")
    private String registradoPorEntradaId;

    @Column(name = "registrado_por_saida_id")
    private String registradoPorSaidaId;

    @Column(name = "entrada_em", nullable = false)
    private LocalDateTime entradaEm;

    @Column(name = "saida_em")
    private LocalDateTime saidaEm;
}
