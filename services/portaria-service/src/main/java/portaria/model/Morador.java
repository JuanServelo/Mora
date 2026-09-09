package portaria.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "moradores")
public class Morador extends Usuario {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bloco_id")
    private Bloco bloco;

    @JsonProperty("blocoNome")
    public String getBlocoNome() {
        return bloco != null ? bloco.getNome() : null;
    }

    @JsonProperty("apartamentoNumero")
    public String getApartamentoNumero() {
        return apartamento != null ? apartamento.getNumero() : null;
    }

    @JsonProperty("apartamentoId")
    public String getApartamentoId() {
        return apartamento != null ? apartamento.getId().toString() : null;
    }
}
