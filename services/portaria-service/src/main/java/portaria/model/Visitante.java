package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoVisita;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "visitantes")
public class Visitante extends Usuario {

    private String documento;

    @Column(name = "motivo_visita")
    private String motivoVisita;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_visita")
    private TipoVisita tipoVisita = TipoVisita.VISITA;

    private String empresa;
    private String destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloco_id")
    private Bloco bloco;

    @Enumerated(EnumType.STRING)
    private StatusAcesso status;

    @Column(name = "horario_entrada")
    private LocalDateTime horarioEntrada;

    @Column(name = "horario_saida")
    private LocalDateTime horarioSaida;
}
