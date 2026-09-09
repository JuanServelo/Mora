package portaria.dto;

import portaria.model.PreAutorizacao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PreAutorizacaoResponseDTO(
        String id,
        UUID moradorId,
        String condominioId,
        String nomeVisitante,
        String cpfVisitante,
        LocalDate validadeInicio,
        LocalDate validadeFim,
        String observacoes,
        boolean ativo,
        LocalDateTime criadoEm
) {
    public static PreAutorizacaoResponseDTO fromEntity(PreAutorizacao p) {
        return new PreAutorizacaoResponseDTO(
                p.getId(),
                p.getMoradorId(),
                p.getCondominioId(),
                p.getNomeVisitante(),
                p.getCpfVisitante(),
                p.getValidadeInicio(),
                p.getValidadeFim(),
                p.getObservacoes(),
                p.isAtivo(),
                p.getCriadoEm()
        );
    }
}
