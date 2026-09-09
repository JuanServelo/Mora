package portaria.dto;

import lombok.Builder;
import lombok.Data;
import portaria.model.Veiculo;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
import java.time.LocalDateTime;

@Data
@Builder
public class VeiculoResponseDTO {

    private String id;
    private String placa;
    private String modelo;
    private String cor;
    private String obs;
    private CategoriaVeiculo categoria;
    private String proprietarioId;
    private String proprietarioNome;
    private String proprietarioCpf;
    private TipoProprietario tipoProprietario;
    private String visitanteNome;
    private Boolean visitanteSaiu;
    private String vagaId;
    private String vagaNumero;
    private String vagaLocalizacao;
    private String apartamentoId;
    private String apartamentoNumero;
    private StatusAcesso status;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataSaida;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static VeiculoResponseDTOBuilder builderFromEntity(Veiculo v) {
        VeiculoResponseDTOBuilder b = VeiculoResponseDTO.builder()
                .id(v.getId())
                .placa(v.getPlaca())
                .modelo(v.getModelo())
                .cor(v.getCor())
                .obs(v.getObs())
                .categoria(v.getCategoria())
                .proprietarioId(v.getProprietarioId())
                .proprietarioNome(v.getProprietarioNome())
                .proprietarioCpf(v.getProprietarioCpf())
                .tipoProprietario(v.getTipoProprietario())
                .status(v.getStatus())
                .dataEntrada(v.getDataEntrada())
                .dataSaida(v.getDataSaida())
                .criadoEm(v.getCriadoEm())
                .atualizadoEm(v.getAtualizadoEm());

        if (v.getVaga() != null) {
            b.vagaId(v.getVaga().getId())
             .vagaNumero(v.getVaga().getNumero())
             .vagaLocalizacao(v.getVaga().getLocalizacao());

            if (v.getVaga().getApartamento() != null) {
                b.apartamentoId(v.getVaga().getApartamento().getId().toString())
                 .apartamentoNumero(v.getVaga().getApartamento().getNumero());
            }
        }

        return b;
    }

    public static VeiculoResponseDTO fromEntity(Veiculo v) {
        return builderFromEntity(v).build();
    }
}
