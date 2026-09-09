package portaria.dto;

import lombok.Builder;
import lombok.Data;
import portaria.model.MovimentacaoVeiculo;

import java.time.LocalDateTime;

@Data
@Builder
public class MovimentacaoVeiculoResponseDTO {

    private String id;
    private String placa;
    private String modelo;
    private String veiculoId;
    private String vagaId;
    private String vinculadoNome;
    private String registradoPorEntradaId;
    private String registradoPorSaidaId;
    private LocalDateTime entradaEm;
    private LocalDateTime saidaEm;

    public static MovimentacaoVeiculoResponseDTO fromEntity(MovimentacaoVeiculo m) {
        return MovimentacaoVeiculoResponseDTO.builder()
                .id(m.getId())
                .placa(m.getPlaca())
                .modelo(m.getModelo())
                .veiculoId(m.getVeiculoId())
                .vagaId(m.getVagaId())
                .vinculadoNome(m.getVinculadoNome())
                .registradoPorEntradaId(m.getRegistradoPorEntradaId())
                .registradoPorSaidaId(m.getRegistradoPorSaidaId())
                .entradaEm(m.getEntradaEm())
                .saidaEm(m.getSaidaEm())
                .build();
    }
}
