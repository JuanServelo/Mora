package portaria.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "blocos", uniqueConstraints = {
    @UniqueConstraint(name = "blocos_nome_condominioId_key", columnNames = {"nome", "condominioId"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bloco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Nome do bloco é obrigatório")
    private String nome;

    @Column(name = "`condominioId`")
    private String condominioId;

    private String descricao;

    private Integer andares;

    @Column(name = "`apartamentosPorAndar`")
    private Integer apartamentosPorAndar;

    private boolean ativo = true;

    @Column(name = "`criadoEm`")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "`atualizadoEm`")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
