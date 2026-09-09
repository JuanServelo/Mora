package comunicacao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import comunicacao.model.enums.CategoriaArtigo;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "artigos_conhecimento")
public class ArtigoConhecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Título é obrigatório")
    private String titulo;

    @NotBlank(message = "Conteúdo é obrigatório")
    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    private CategoriaArtigo categoria;

    private String autor;

    @Column(name = "`condominioId`")
    private String condominioId;

    private boolean publicado = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
