package portaria.dto.atendimento;

import lombok.Builder;
import lombok.Data;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoVisita;

import java.time.LocalDateTime;

@Data
@Builder
public class VisitanteResumoDTO {

    private String id;
    private String nome;
    private String documento;
    private String telefone;
    private TipoVisita tipoVisita;
    private StatusAcesso status;
    private LocalDateTime horarioEntrada;
    private LocalDateTime horarioSaida;
    private String apartamentoId;
    private String apartamentoNumero;
    private String blocoNome;
    private String empresa;
    private String destino;
    private String condominioId;
    // Preenchido quando o visitante sai mas tem veículo ainda dentro (AJUSTE 5)
    private String veiculoDentroPlaca;
    private String vagaDentroNumero;

    public static VisitanteResumoDTOBuilder builderFromEntity(Visitante v) {
        VisitanteResumoDTOBuilder b = VisitanteResumoDTO.builder()
                .id(v.getId())
                .nome(v.getNome())
                .documento(v.getDocumento())
                .telefone(v.getTelefone())
                .tipoVisita(v.getTipoVisita())
                .status(v.getStatus())
                .horarioEntrada(v.getHorarioEntrada())
                .horarioSaida(v.getHorarioSaida())
                .empresa(v.getEmpresa())
                .destino(v.getDestino())
                .condominioId(v.getCondominioId());

        if (v.getApartamento() != null) {
            b.apartamentoId(v.getApartamento().getId().toString())
             .apartamentoNumero(v.getApartamento().getNumero());
        }
        if (v.getBloco() != null) {
            b.blocoNome(v.getBloco().getNome());
        }

        return b;
    }

    public static VisitanteResumoDTO fromEntity(Visitante v) {
        return builderFromEntity(v).build();
    }
}
