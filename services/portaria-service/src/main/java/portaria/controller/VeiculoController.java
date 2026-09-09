package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.AlterarVagaDTO;
import portaria.dto.CriarVeiculoDTO;
import portaria.dto.MovimentacaoVeiculoResponseDTO;
import portaria.dto.VeiculoResponseDTO;
import portaria.service.VeiculoService;

import java.util.List;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;

    @PostMapping("/cadastrar")
    public ResponseEntity<VeiculoResponseDTO> cadastrar(@Valid @RequestBody CriarVeiculoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veiculoService.cadastrar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody CriarVeiculoDTO dto) {
        return ResponseEntity.ok(veiculoService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/vaga")
    public ResponseEntity<VeiculoResponseDTO> alterarVaga(
            @PathVariable String id,
            @RequestBody AlterarVagaDTO dto) {
        return ResponseEntity.ok(veiculoService.alterarVaga(id, dto));
    }

    @PostMapping("/{id}/entrada")
    public ResponseEntity<VeiculoResponseDTO> registrarEntrada(
            @PathVariable String id,
            @RequestParam(required = false) String vagaId) {
        return ResponseEntity.ok(veiculoService.registrarEntrada(id, vagaId));
    }

    @PostMapping("/entrada/placa/{placa}")
    public ResponseEntity<VeiculoResponseDTO> registrarEntradaPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.registrarEntradaPorPlaca(placa));
    }

    @PostMapping("/{id}/saida")
    public ResponseEntity<VeiculoResponseDTO> registrarSaida(@PathVariable String id) {
        return ResponseEntity.ok(veiculoService.registrarSaida(id));
    }

    @GetMapping
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(veiculoService.listarTodos());
    }

    @GetMapping("/dentro")
    public ResponseEntity<List<VeiculoResponseDTO>> listarDentro() {
        return ResponseEntity.ok(veiculoService.listarDentro());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/proprietario/{proprietarioId}")
    public ResponseEntity<List<VeiculoResponseDTO>> listarPorProprietario(@PathVariable String proprietarioId) {
        return ResponseEntity.ok(veiculoService.listarPorProprietario(proprietarioId));
    }

    // ─── Portaria: entrada avulsa (veículo não cadastrado) ───────────────────

    @PostMapping("/entrada/avulsa/{placa}")
    public ResponseEntity<MovimentacaoVeiculoResponseDTO> registrarEntradaAvulsa(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.registrarEntradaAvulsa(placa));
    }

    @PostMapping("/saida/placa/{placa}")
    public ResponseEntity<MovimentacaoVeiculoResponseDTO> registrarSaidaPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.registrarSaidaPorPlaca(placa));
    }

    @GetMapping("/dentro-portaria")
    public ResponseEntity<List<MovimentacaoVeiculoResponseDTO>> listarDentroPortaria() {
        return ResponseEntity.ok(veiculoService.listarDentroPortaria());
    }

    @GetMapping("/historico-acesso")
    public ResponseEntity<List<MovimentacaoVeiculoResponseDTO>> listarHistoricoAcesso(
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(veiculoService.listarHistoricoAcesso(placa, dataInicio, dataFim, status));
    }
}
