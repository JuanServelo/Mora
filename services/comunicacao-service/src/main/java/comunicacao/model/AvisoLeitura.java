package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "aviso_leituras",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aviso_id", "usuario_id"}))
public class AvisoLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aviso_id", nullable = false)
    private UUID avisoId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "lido_em")
    private LocalDateTime lidoEm = LocalDateTime.now();
}
