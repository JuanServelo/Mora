package portaria.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movimentacoes_chaves")
public class MovimentacaoChave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "chave_id", nullable = false)
    private String chaveId;

    @Column(name = "chave_nome")
    private String chaveNome;

    @Column(name = "responsavel_id")
    private String responsavelId;

    @Column(name = "nome_responsavel")
    private String nomeResponsavel;

    // Snapshot do perfil no momento da retirada (RN-08/RN-14)
    @Column(name = "perfil_responsavel")
    private String perfilResponsavel;

    @Column(name = "data_retirada", nullable = false)
    private LocalDateTime dataRetirada;

    @Column(name = "data_devolucao")
    private LocalDateTime dataDevolucao;

    @Column(name = "registrado_por_id")
    private String registradoPorId;

    @Column(name = "registrado_por_nome")
    private String registradoPorNome;

    @Column(name = "`condominioId`")
    private String condominioId;
}
