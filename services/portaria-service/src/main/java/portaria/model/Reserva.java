package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(name = "solicitante_id")
    private UUID solicitanteId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_comum_id")
    private AreaComum areaComum;

    @NotNull
    private LocalDate dataInicio;

    @NotNull
    private LocalDate dataFim;

    private LocalTime horaInicio;

    private LocalTime horaFim;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    private StatusReserva status = StatusReserva.PENDENTE;

    @Column(name = "motivo_recusa", length = 500)
    private String motivoRecusa;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
