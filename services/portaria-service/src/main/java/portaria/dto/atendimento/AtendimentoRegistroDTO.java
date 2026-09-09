package portaria.dto.atendimento;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtendimentoRegistroDTO {

    /** "VISITA" ou "SERVICO". */
    @NotBlank(message = "Tipo de visita é obrigatório")
    private String tipoVisita;

    /** ID do visitante já cadastrado; nulo para cadastrar novo. */
    private String pessoaId;

    // ─── Campos de cadastro (obrigatórios quando pessoaId é nulo) ─────────────

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    /** CPF ou RG, normalizado (sem pontuação) antes de gravar. */
    @NotBlank(message = "Documento é obrigatório")
    private String documento;

    private String telefone;
    private String obs;

    // ─── VISITA ───────────────────────────────────────────────────────────────

    /** UUID do apartamento visitado. Obrigatório para VISITA. */
    private String apartamentoId;

    /** UUID do morador da unidade que recebe o visitante (opcional). */
    private String moradorResponsavelId;

    // ─── SERVICO ──────────────────────────────────────────────────────────────

    private String empresa;
    private String destino;

    // ─── Veículo (opcional) ───────────────────────────────────────────────────

    private AtendimentoVeiculoDTO veiculo;
}
