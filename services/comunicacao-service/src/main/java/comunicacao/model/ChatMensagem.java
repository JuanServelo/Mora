package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "chat_mensagens")
public class ChatMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(name = "remetente_id")
    private UUID remetenteId;

    @Column(name = "destinatario_id")
    private UUID destinatarioId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String texto;

    private boolean lida = false;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm = LocalDateTime.now();

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;
}
