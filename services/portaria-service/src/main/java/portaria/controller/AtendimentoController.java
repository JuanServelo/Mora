package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.atendimento.AtendimentoRegistroDTO;
import portaria.dto.atendimento.AtendimentoResponseDTO;
import portaria.dto.atendimento.VagaStatusDTO;
import portaria.dto.atendimento.VisitanteResumoDTO;
import portaria.model.enums.TipoVisita;
import portaria.service.AtendimentoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/atendimento")
@RequiredArgsConstructor
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    @GetMapping("/{id}")
    public ResponseEntity<VisitanteResumoDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    /**
     * Busca visitantes/terceiros por nome ou documento.
     * @param q    termo de busca (parcial, case-insensitive)
     * @param tipo VISITA ou SERVICO
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<VisitanteResumoDTO>> buscar(
            @RequestParam String q,
            @RequestParam TipoVisita tipo) {
        return ResponseEntity.ok(atendimentoService.buscar(q, tipo));
    }

    /**
     * Retorna as vagas de um apartamento com status de ocupação.
     * Usado para popular o seletor de vaga no fluxo de Visitante.
     */
    @GetMapping("/vagas-unidade/{apartamentoId}")
    public ResponseEntity<List<VagaStatusDTO>> vagasUnidade(
            @PathVariable UUID apartamentoId) {
        return ResponseEntity.ok(atendimentoService.vagasUnidade(apartamentoId));
    }

    /**
     * Registra o atendimento completo: pessoa + veículo (opcional) + entrada.
     * Operação atômica: falha em qualquer etapa reverte tudo.
     */
    @PostMapping("/registrar")
    public ResponseEntity<AtendimentoResponseDTO> registrar(
            @Valid @RequestBody AtendimentoRegistroDTO dto) {
        return ResponseEntity.ok(atendimentoService.registrar(dto));
    }

    /**
     * Retorna visitantes/terceiros atualmente dentro do condomínio (status=DENTRO).
     */
    @GetMapping("/dentro")
    public ResponseEntity<List<VisitanteResumoDTO>> dentro() {
        return ResponseEntity.ok(atendimentoService.dentro());
    }

    /**
     * Histórico de visitantes/terceiros com filtros opcionais.
     */
    @GetMapping("/historico")
    public ResponseEntity<List<VisitanteResumoDTO>> historico(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String tipoVisita,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(atendimentoService.historico(nome, tipoVisita, status));
    }

    /**
     * Registra a saída de um visitante/terceiro.
     */
    @PostMapping("/{id}/saida")
    public ResponseEntity<VisitanteResumoDTO> registrarSaida(@PathVariable String id) {
        return ResponseEntity.ok(atendimentoService.registrarSaida(id));
    }
}
