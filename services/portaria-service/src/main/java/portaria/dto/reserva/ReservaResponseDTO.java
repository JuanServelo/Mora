package portaria.dto.reserva;

import portaria.model.Reserva;
import portaria.model.enums.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ReservaResponseDTO(
        String id,
        UUID solicitanteId,
        String condominioId,
        String areaComunId,
        String areaComunNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        BigDecimal valorTotal,
        StatusReserva status,
        String motivoRecusa,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ReservaResponseDTO fromEntity(Reserva r) {
        return new ReservaResponseDTO(
                r.getId(),
                r.getSolicitanteId(),
                r.getCondominioId(),
                r.getAreaComum() != null ? r.getAreaComum().getId() : null,
                r.getAreaComum() != null ? r.getAreaComum().getNome() : null,
                r.getDataInicio(),
                r.getDataFim(),
                r.getHoraInicio(),
                r.getHoraFim(),
                r.getValorTotal(),
                r.getStatus(),
                r.getMotivoRecusa(),
                r.getObservacoes(),
                r.getCriadoEm(),
                r.getAtualizadoEm()
        );
    }
}
