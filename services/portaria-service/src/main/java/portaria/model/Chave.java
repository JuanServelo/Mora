package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import portaria.model.enums.TipoLocal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chaves")
public class Chave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome da chave é obrigatório")
    @Column(name = "nome_chave")
    private String nomeChave;

    // Lowercase + trimmed, usado para unicidade por local (RN-04)
    @Column(name = "nome_normalizado")
    private String nomeNormalizado;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "local_id")
    private String localId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_local")
    private TipoLocal tipoLocal;

    @Column(name = "local_nome")
    private String localNome;

    @Column(name = "`condominioId`")
    private String condominioId;

    private boolean disponivel = true;

    @Column(name = "`criadoEm`")
    private LocalDateTime criadoEm = LocalDateTime.now();
}
