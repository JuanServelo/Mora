package portaria.dto.aluguel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import portaria.model.enums.ModalidadeAluguel;

import java.time.LocalDate;

public record AluguelVagaRequestDTO(

        @NotBlank(message = "ID da vaga é obrigatório")
        String vagaId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "Data de fim é obrigatória")
        LocalDate dataFim,

        @NotNull(message = "Modalidade é obrigatória (DIARIA ou MENSAL)")
        ModalidadeAluguel modalidade
) {}
