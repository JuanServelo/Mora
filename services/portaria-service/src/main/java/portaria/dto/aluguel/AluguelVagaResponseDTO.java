package portaria.dto.aluguel;

import lombok.Builder;
import portaria.model.AluguelVaga;
import portaria.model.enums.ModalidadeAluguel;
import portaria.model.enums.StatusAluguel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AluguelVagaResponseDTO(
        String id,
        UUID solicitanteId,
        UUID proprietarioId,
        String vagaId,
        String vagaNumero,
        LocalDate dataInicio,
        LocalDate dataFim,
        ModalidadeAluguel modalidade,
        BigDecimal valorTotal,
        BigDecimal valorPenalidade,
        StatusAluguel status,
        String motivoRecusa,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static AluguelVagaResponseDTO fromEntity(AluguelVaga a) {
        return AluguelVagaResponseDTO.builder()
                .id(a.getId())
                .solicitanteId(a.getSolicitanteId())
                .proprietarioId(a.getProprietarioId())
                .vagaId(a.getVaga().getId())
                .vagaNumero(a.getVaga().getNumero())
                .dataInicio(a.getDataInicio())
                .dataFim(a.getDataFim())
                .modalidade(a.getModalidade())
                .valorTotal(a.getValorTotal())
                .valorPenalidade(a.getValorPenalidade())
                .status(a.getStatus())
                .motivoRecusa(a.getMotivoRecusa())
                .criadoEm(a.getCriadoEm())
                .atualizadoEm(a.getAtualizadoEm())
                .build();
    }
}
