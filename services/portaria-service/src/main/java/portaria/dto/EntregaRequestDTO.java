package portaria.dto;

import lombok.Data;

@Data
public class EntregaRequestDTO {

    private Long destinatarioId;
    private String moradorId;       // UUID do morador (preferencial — preenche nome/bloco/apt automaticamente)
    private String destinatarioNome;
    private String bloco;
    private String apartamento;
    private String descricao;
    private String remetente;
    private String status;           // "PENDENTE" | "RETIRADA"
    private String observacoes;
    private String dataRecebimento;  // "yyyy-MM-dd"
    private String dataRetirada;     // "yyyy-MM-dd" ou null
    private String recebedorNome;
}
