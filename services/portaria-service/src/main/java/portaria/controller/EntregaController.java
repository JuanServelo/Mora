package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.EntregaRequestDTO;
import portaria.dto.EntregaResponseDTO;
import portaria.security.CondominioUtils;
import portaria.service.EntregaService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @PostMapping("/cadastrar")
    public ResponseEntity<EntregaResponseDTO> cadastrar(@RequestBody EntregaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntregaResponseDTO.fromEntity(entregaService.cadastrar(dto)));
    }

    @PutMapping("/{id}")
    public EntregaResponseDTO atualizar(@PathVariable String id, @RequestBody EntregaRequestDTO dto) {
        return EntregaResponseDTO.fromEntity(entregaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        entregaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<EntregaResponseDTO> listarTodas() {
        return entregaService.listarTodas(CondominioUtils.condominioIdEfetivo()).stream()
                .map(EntregaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/pendentes")
    public List<EntregaResponseDTO> listarPendentes() {
        return entregaService.listarPendentes(CondominioUtils.condominioIdEfetivo()).stream()
                .map(EntregaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public EntregaResponseDTO buscarPorId(@PathVariable String id) {
        return EntregaResponseDTO.fromEntity(entregaService.buscarPorId(id));
    }

    @GetMapping("/morador/{moradorId}")
    public List<EntregaResponseDTO> listarPorMorador(@PathVariable UUID moradorId,
                                                     @RequestParam(required = false) boolean pendentesOnly) {
        if (pendentesOnly) {
            return entregaService.listarPendentesPorMorador(moradorId).stream()
                    .map(EntregaResponseDTO::fromEntity).toList();
        }
        return entregaService.listarPorMorador(moradorId).stream()
                .map(EntregaResponseDTO::fromEntity).toList();
    }

    @PostMapping("/{id}/retirar")
    public EntregaResponseDTO retirar(@PathVariable String id, @RequestBody(required = false) String recebedor) {
        return EntregaResponseDTO.fromEntity(entregaService.retirar(id, recebedor));
    }
}
