package portaria.dto.chave;

import portaria.model.Chave;
import portaria.model.MovimentacaoChave;
import portaria.model.enums.TipoLocal;
import java.time.LocalDateTime;

public record ChaveResponseDTO(
    String id,
    String nomeChave,
    String descricao,
    String localId,
    TipoLocal tipoLocal,
    String localNome,
    String condominioId,
    boolean disponivel,
    String possuidorNome,
    String possuidorPerfil,
    LocalDateTime retiradaEm
) {
    public static ChaveResponseDTO from(Chave c, MovimentacaoChave aberto) {
        return new ChaveResponseDTO(
            c.getId(), c.getNomeChave(), c.getDescricao(),
            c.getLocalId(), c.getTipoLocal(), c.getLocalNome(),
            c.getCondominioId(), c.isDisponivel(),
            aberto != null ? aberto.getNomeResponsavel() : null,
            aberto != null ? aberto.getPerfilResponsavel() : null,
            aberto != null ? aberto.getDataRetirada() : null
        );
    }
}
