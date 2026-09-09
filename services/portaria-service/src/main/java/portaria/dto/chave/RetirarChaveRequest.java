package portaria.dto.chave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.TipoResponsavel;

@Data
public class RetirarChaveRequest {

    @NotBlank(message = "ID do responsável é obrigatório")
    private String responsavelId;

    @NotNull(message = "Tipo do responsável é obrigatório (MORADOR ou FUNCIONARIO)")
    private TipoResponsavel tipoResponsavel;

    /** Nome do responsável, quando não está cadastrado no portaria-service. */
    private String nomeResponsavel;
}
