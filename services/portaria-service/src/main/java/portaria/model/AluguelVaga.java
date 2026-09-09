package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.ModalidadeAluguel;
import portaria.model.enums.StatusAluguel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "alugueis_vagas")
public class AluguelVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;

    @NotNull
    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;

    @NotNull
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @NotNull
    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ModalidadeAluguel modalidade;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_penalidade", precision = 10, scale = 2)
    private BigDecimal valorPenalidade;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusAluguel status = StatusAluguel.PENDENTE;

    @Column(name = "motivo_recusa", length = 500)
    private String motivoRecusa;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
