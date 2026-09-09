package portaria.dto.aluguel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DisponibilidadeVagaRequestDTO(

        @NotBlank(message = "ID da vaga é obrigatório")
        String vagaId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "Data de fim é obrigatória")
        LocalDate dataFim
) {}
