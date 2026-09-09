package portaria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.TipoProprietario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarVeiculoDTO {

    @NotBlank(message = "Placa é obrigatória")
    private String placa;

    private String modelo;

    private String cor;

    private String obs;

    /**
     * Tipo de vínculo (Morador / Visitante / Serviço).
     * Determina quais campos são obrigatórios e a categoria derivada.
     * Quando presente, categoria é ignorada e computada server-side.
     */
    private TipoProprietario tipoProprietario;

    /**
     * Mantido para compatibilidade com cadastros legados que ainda enviam categoria.
     * Quando tipoProprietario estiver presente, este campo é ignorado.
     */
    private CategoriaVeiculo categoria;

    /** Obrigatório para MORADOR e FUNCIONARIO; anfitrião para VISITANTE. */
    private String proprietarioId;

    /** Obrigatório para MORADOR; não exibido/enviado para VISITANTE e FUNCIONARIO. */
    private String vagaId;
}
