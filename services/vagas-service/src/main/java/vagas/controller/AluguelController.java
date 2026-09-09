package vagas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vagas.dto.*;
import vagas.exception.OperacaoInvalidaException;
import vagas.model.enums.StatusAluguel;
import vagas.security.AuthContext;
import vagas.security.JwtClaims;
import vagas.service.AluguelService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alugueis")
@RequiredArgsConstructor
public class AluguelController {

    private final AluguelService aluguelService;

    @PostMapping("/disponibilizar")
    public ResponseEntity<DisponibilidadeResponseDTO> disponibilizar(
            @Valid @RequestBody DisponibilidadeRequestDTO dto) {

        var resultado = aluguelService.disponibilizar(dto, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisponibilidadeResponseDTO.fromEntity(resultado));
    }

    @GetMapping("/disponiveis")
    public List<DisponibilidadeResponseDTO> listarDisponiveis() {
        return aluguelService.listarDisponibilidadesAtivas().stream()
                .map(DisponibilidadeResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/minhas-disponibilidades")
    public List<DisponibilidadeResponseDTO> minhasDisponibilidades() {
        return aluguelService.listarMinhasDisponibilidades(currentUserId()).stream()
                .map(DisponibilidadeResponseDTO::fromEntity)
                .toList();
    }

    @DeleteMapping("/disponibilidades/{id}")
    public ResponseEntity<Void> cancelarDisponibilidade(@PathVariable String id) {
        aluguelService.cancelarDisponibilidade(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/solicitar")
    public ResponseEntity<AluguelResponseDTO> solicitar(
            @Valid @RequestBody AluguelRequestDTO dto) {

        var aluguel = aluguelService.solicitar(dto, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AluguelResponseDTO.fromEntity(aluguel));
    }

    @PostMapping("/{id}/aprovar")
    public AluguelResponseDTO aprovar(@PathVariable String id) {
        return AluguelResponseDTO.fromEntity(aluguelService.aprovar(id, currentUserId()));
    }

    @PostMapping("/{id}/recusar")
    public AluguelResponseDTO recusar(
            @PathVariable String id,
            @Valid @RequestBody RecusaRequestDTO recusaDTO) {

        return AluguelResponseDTO.fromEntity(
                aluguelService.recusar(id, currentUserId(), recusaDTO.justificativa()));
    }

    @PostMapping("/{id}/cancelar")
    public AluguelResponseDTO cancelar(@PathVariable String id) {
        return AluguelResponseDTO.fromEntity(aluguelService.cancelar(id, currentUserId()));
    }

    @GetMapping("/historico/solicitante")
    public List<AluguelResponseDTO> historicoComoSolicitante(
            @RequestParam(required = false) StatusAluguel status) {

        UUID id = currentUserId();
        var lista = (status != null)
                ? aluguelService.historicoPorSolicitanteEStatus(id, status)
                : aluguelService.historicoPorSolicitante(id);
        return lista.stream().map(AluguelResponseDTO::fromEntity).toList();
    }

    @GetMapping("/historico/proprietario")
    public List<AluguelResponseDTO> historicoComoProprietario(
            @RequestParam(required = false) StatusAluguel status) {

        UUID id = currentUserId();
        var lista = (status != null)
                ? aluguelService.historicoPorProprietarioEStatus(id, status)
                : aluguelService.historicoPorProprietario(id);
        return lista.stream().map(AluguelResponseDTO::fromEntity).toList();
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado.");
        }
        try {
            return UUID.fromString(claims.authUserId());
        } catch (IllegalArgumentException e) {
            throw new OperacaoInvalidaException("ID de usuário inválido no token.");
        }
    }
}
