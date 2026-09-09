package portaria.dto.aluguel;

import lombok.Builder;
import portaria.model.DisponibilidadeVaga;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DisponibilidadeVagaResponseDTO(
        String id,
        UUID proprietarioId,
        String vagaId,
        String vagaNumero,
        String vagaLocalizacao,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean ativa,
        LocalDateTime criadoEm
) {
    public static DisponibilidadeVagaResponseDTO fromEntity(DisponibilidadeVaga d) {
        return DisponibilidadeVagaResponseDTO.builder()
                .id(d.getId())
                .proprietarioId(d.getProprietarioId())
                .vagaId(d.getVaga().getId())
                .vagaNumero(d.getVaga().getNumero())
                .vagaLocalizacao(d.getVaga().getLocalizacao())
                .dataInicio(d.getDataInicio())
                .dataFim(d.getDataFim())
                .ativa(d.isAtiva())
                .criadoEm(d.getCriadoEm())
                .build();
    }
}
