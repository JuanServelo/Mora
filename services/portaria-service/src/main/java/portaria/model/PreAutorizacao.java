package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "pre_autorizacoes")
public class PreAutorizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @Column(name = "morador_id")
    private UUID moradorId;

    @Column(name = "`condominioId`")
    private String condominioId;

    @NotBlank
    @Column(name = "nome_visitante")
    private String nomeVisitante;

    @Column(name = "cpf_visitante", length = 14)
    private String cpfVisitante;

    @NotNull
    @Column(name = "validade_inicio")
    private LocalDate validadeInicio;

    @NotNull
    @Column(name = "validade_fim")
    private LocalDate validadeFim;

    @Column(length = 500)
    private String observacoes;

    private boolean ativo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
