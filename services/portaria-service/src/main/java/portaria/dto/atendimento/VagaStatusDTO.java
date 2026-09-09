package portaria.dto.atendimento;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VagaStatusDTO {

    private String id;
    private String numero;
    private String tipo;
    private String localizacao;

    /** true = nenhum veículo com entrada em aberto; false = ocupada. */
    private boolean disponivel;

    /** Placa do veículo que está ocupando a vaga (null quando disponivel=true). */
    private String ocupadaPorPlaca;

    /** Data/hora formatada da entrada do veículo ocupante (null quando disponivel=true). */
    private String ocupadaDesde;
}
