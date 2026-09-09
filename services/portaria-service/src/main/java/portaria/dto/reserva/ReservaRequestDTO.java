package portaria.dto.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequestDTO(
        String areaComunId,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        String observacoes
) {}
