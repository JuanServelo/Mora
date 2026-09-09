package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import comunicacao.model.enums.PublicoAlvo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "avisos")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Título é obrigatório")
    private String titulo;

    @NotBlank(message = "Mensagem é obrigatória")
    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @NotNull(message = "Data inicial é obrigatória")
    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @NotNull(message = "Data final é obrigatória")
    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "publico_alvo")
    private PublicoAlvo publicoAlvo = PublicoAlvo.TODOS;

    @Column(name = "`condominioId`")
    private String condominioId;

    private String autor;

    private boolean publicado = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
