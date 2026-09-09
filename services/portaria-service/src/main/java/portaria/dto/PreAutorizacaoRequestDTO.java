package portaria.dto;

import java.time.LocalDate;

public record PreAutorizacaoRequestDTO(
        String nomeVisitante,
        String cpfVisitante,
        LocalDate validadeInicio,
        LocalDate validadeFim,
        String observacoes
) {}
