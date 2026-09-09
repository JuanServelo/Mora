package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.aluguel.*;
import portaria.exception.OperacaoInvalidaException;
import portaria.model.enums.StatusAluguel;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.AluguelService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alugueis")
@RequiredArgsConstructor
public class AluguelController {

    private final AluguelService aluguelService;

    @PostMapping("/disponibilizar")
    public ResponseEntity<DisponibilidadeVagaResponseDTO> disponibilizar(
            @Valid @RequestBody DisponibilidadeVagaRequestDTO dto) {
        var resultado = aluguelService.disponibilizar(dto, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisponibilidadeVagaResponseDTO.fromEntity(resultado));
    }

    @GetMapping("/disponiveis")
    public List<DisponibilidadeVagaResponseDTO> listarDisponiveis() {
        return aluguelService.listarDisponibilidadesAtivas(CondominioUtils.condominioIdEfetivo()).stream()
                .map(DisponibilidadeVagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/minhas-disponibilidades")
    public List<DisponibilidadeVagaResponseDTO> minhasDisponibilidades() {
        return aluguelService.listarMinhasDisponibilidades(currentUserId()).stream()
                .map(DisponibilidadeVagaResponseDTO::fromEntity)
                .toList();
    }

    @DeleteMapping("/disponibilidades/{id}")
    public ResponseEntity<Void> cancelarDisponibilidade(@PathVariable String id) {
        aluguelService.cancelarDisponibilidade(id, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/solicitar")
    public ResponseEntity<AluguelVagaResponseDTO> solicitar(
            @Valid @RequestBody AluguelVagaRequestDTO dto) {
        var aluguel = aluguelService.solicitar(dto, currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AluguelVagaResponseDTO.fromEntity(aluguel));
    }

    @PostMapping("/{id}/aprovar")
    public AluguelVagaResponseDTO aprovar(@PathVariable String id) {
        return AluguelVagaResponseDTO.fromEntity(aluguelService.aprovar(id, currentUserId()));
    }

    @PostMapping("/{id}/recusar")
    public AluguelVagaResponseDTO recusar(
            @PathVariable String id,
            @Valid @RequestBody RecusaAluguelRequestDTO recusaDTO) {
        return AluguelVagaResponseDTO.fromEntity(
                aluguelService.recusar(id, currentUserId(), recusaDTO.justificativa()));
    }

    @PostMapping("/{id}/cancelar")
    public AluguelVagaResponseDTO cancelar(@PathVariable String id) {
        return AluguelVagaResponseDTO.fromEntity(aluguelService.cancelar(id, currentUserId()));
    }

    @GetMapping("/historico/solicitante")
    public List<AluguelVagaResponseDTO> historicoComoSolicitante(
            @RequestParam(required = false) StatusAluguel status) {
        UUID id = currentUserId();
        var lista = (status != null)
                ? aluguelService.historicoPorSolicitanteEStatus(id, status)
                : aluguelService.historicoPorSolicitante(id);
        return lista.stream().map(AluguelVagaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/historico/proprietario")
    public List<AluguelVagaResponseDTO> historicoComoProprietario(
            @RequestParam(required = false) StatusAluguel status) {
        UUID id = currentUserId();
        var lista = (status != null)
                ? aluguelService.historicoPorProprietarioEStatus(id, status)
                : aluguelService.historicoPorProprietario(id);
        return lista.stream().map(AluguelVagaResponseDTO::fromEntity).toList();
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
