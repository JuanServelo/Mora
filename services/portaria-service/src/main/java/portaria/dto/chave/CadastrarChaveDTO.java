package portaria.dto.chave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.TipoLocal;

@Data
public class CadastrarChaveDTO {

    @NotBlank(message = "Local é obrigatório")
    private String localId;

    @NotNull(message = "Tipo do local é obrigatório (BLOCO ou AREA_COMUM)")
    private TipoLocal tipoLocal;

    @NotBlank(message = "Nome da chave é obrigatório")
    private String nomeChave;

    private String descricao;
}
