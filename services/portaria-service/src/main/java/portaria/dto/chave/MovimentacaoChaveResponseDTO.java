package portaria.dto.chave;

import portaria.model.MovimentacaoChave;
import java.time.LocalDateTime;

public record MovimentacaoChaveResponseDTO(
    String id,
    String nomeResponsavel,
    String perfilResponsavel,
    LocalDateTime dataRetirada,
    LocalDateTime dataDevolucao,
    String registradoPorNome,
    String status
) {
    public static MovimentacaoChaveResponseDTO from(MovimentacaoChave m) {
        return new MovimentacaoChaveResponseDTO(
            m.getId(),
            m.getNomeResponsavel(),
            m.getPerfilResponsavel(),
            m.getDataRetirada(),
            m.getDataDevolucao(),
            m.getRegistradoPorNome(),
            m.getDataDevolucao() == null ? "EM_ABERTO" : "DEVOLVIDA"
        );
    }
}
