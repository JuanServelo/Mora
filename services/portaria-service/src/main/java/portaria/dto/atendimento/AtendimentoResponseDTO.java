package portaria.dto.atendimento;

import lombok.Builder;
import lombok.Data;
import portaria.dto.VeiculoResponseDTO;

@Data
@Builder
public class AtendimentoResponseDTO {

    private VisitanteResumoDTO pessoa;
    private VeiculoResponseDTO veiculo;

    /** Número da vaga ocupada pelo veículo, ou null. */
    private String vagaNumero;

    private String mensagem;
}
