package portaria.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "veiculos")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Placa é obrigatória")
    @Column(unique = true)
    private String placa;

    private String modelo;

    private String cor;

    private String obs;

    /** Cliente dono deste registro. Todo dado de domínio pertence a um condomínio. */
    @Column(name = "`condominioId`")
    private String condominioId;

    @NotNull(message = "Categoria do veículo é obrigatória")
    @Enumerated(EnumType.STRING)
    private CategoriaVeiculo categoria;

    /** Nulo apenas para VEICULO_SERVICO */
    @Column(name = "proprietario_id")
    private String proprietarioId;

    @Column(name = "proprietario_nome")
    private String proprietarioNome;

    @Column(name = "proprietario_cpf")
    private String proprietarioCpf;

    /** Nulo apenas para VEICULO_SERVICO */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proprietario")
    private TipoProprietario tipoProprietario;

    /**
     * Vaga vinculada ao veículo.
     * Obrigatória para CARRO e MOTO; nula para VEICULO_SERVICO.
     * Entrada é bloqueada enquanto a vaga for nula.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vaga_id")
    private Vaga vaga;

    @Enumerated(EnumType.STRING)
    private StatusAcesso status = StatusAcesso.SAIU;

    @Column(name = "data_entrada")
    private LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public boolean isVeiculoServico() {
        return CategoriaVeiculo.VEICULO_SERVICO == this.categoria;
    }
}
