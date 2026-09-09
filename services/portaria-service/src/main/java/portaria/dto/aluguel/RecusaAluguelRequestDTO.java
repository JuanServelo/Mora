package portaria.dto.aluguel;

import jakarta.validation.constraints.NotBlank;

public record RecusaAluguelRequestDTO(
        @NotBlank(message = "Justificativa da recusa é obrigatória")
        String justificativa
) {}
