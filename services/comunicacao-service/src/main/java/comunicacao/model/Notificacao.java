package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import comunicacao.model.enums.TipoNotificacao;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(name = "destinatario_id")
    private UUID destinatarioId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    @NotBlank
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "referencia_id")
    private String referenciaId;

    private boolean lida = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;
}
