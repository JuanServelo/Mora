package portaria.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "entregas")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "destinatario_id")
    private Long destinatarioId;

    @Column(name = "morador_id")
    private UUID moradorId;

    @Column(name = "destinatario_nome")
    private String destinatarioNome;

    // Legacy columns kept to avoid NOT NULL constraint violations in existing DB schema
    @Column(name = "destinatario", nullable = true)
    private String destinatario;

    @Column(name = "nome_entregador", nullable = true)
    private String nomeEntregador;

    @Column(name = "retirada", nullable = false)
    private boolean retirada = false;

    @Column(name = "recebedor_nome")
    private String recebedorNome;

    private String bloco;

    private String apartamento;

    private String descricao;

    /** Cliente dono deste registro. Todo dado de domínio pertence a um condomínio. */
    @Column(name = "`condominioId`")
    private String condominioId;

    private String remetente;

    // "PENDENTE" | "RETIRADA"
    private String status = "PENDENTE";

    private String observacoes;

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
