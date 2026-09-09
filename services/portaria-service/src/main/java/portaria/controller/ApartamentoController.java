package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.ApartamentoRequestDTO;
import portaria.dto.ApartamentoResponseDTO;
import portaria.model.Apartamento;
import portaria.security.CondominioUtils;
import portaria.service.ApartamentoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/apartamentos")
@RequiredArgsConstructor
public class ApartamentoController {

    private final ApartamentoService apartamentoService;

    @PostMapping("/cadastrar")
    public ResponseEntity<ApartamentoResponseDTO> cadastrar(@Valid @RequestBody ApartamentoRequestDTO request) {
        Apartamento salvo = apartamentoService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApartamentoResponseDTO.fromEntity(salvo));
    }

    @GetMapping
    public List<ApartamentoResponseDTO> listarAtivos(@RequestParam(required = false) String condominioId) {
        return apartamentoService.listarAtivos(CondominioUtils.resolverEscopo(condominioId)).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/todos")
    public List<ApartamentoResponseDTO> listarTodos(@RequestParam(required = false) String condominioId) {
        return apartamentoService.listarTodos(CondominioUtils.resolverEscopo(condominioId)).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ApartamentoResponseDTO buscarPorId(@PathVariable UUID id) {
        return ApartamentoResponseDTO.fromEntity(apartamentoService.buscarPorId(id));
    }

    @GetMapping("/bloco/{blocoId}")
    public List<ApartamentoResponseDTO> listarPorBloco(@PathVariable UUID blocoId) {
        return apartamentoService.listarPorBloco(blocoId).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/bloco/{blocoId}/ativos")
    public List<ApartamentoResponseDTO> listarPorBlocoAtivos(@PathVariable UUID blocoId) {
        return apartamentoService.listarPorBlocoAtivos(blocoId).stream()
                .map(ApartamentoResponseDTO::fromEntity)
                .toList();
    }

    @PutMapping("/{id}")
    public ApartamentoResponseDTO atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ApartamentoRequestDTO request
    ) {
        return ApartamentoResponseDTO.fromEntity(apartamentoService.atualizar(id, request));
    }

    @PutMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        apartamentoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        apartamentoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        apartamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
