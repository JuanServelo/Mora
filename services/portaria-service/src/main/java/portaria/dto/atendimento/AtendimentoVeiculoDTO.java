package portaria.dto.atendimento;

import lombok.Data;

@Data
public class AtendimentoVeiculoDTO {

    /** ID do veículo já cadastrado; nulo para cadastrar novo. */
    private String veiculoId;

    /** Placa normalizada (maiúsculas, sem hífen). Obrigatória se veiculoId nulo. */
    private String placa;

    private String modelo;
    private String cor;

    /** ID da vaga escolhida. Obrigatório para VISITA quando há vaga disponível. */
    private String vagaId;
}
