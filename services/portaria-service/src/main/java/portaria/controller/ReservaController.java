package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.reserva.RecusaReservaRequestDTO;
import portaria.dto.reserva.ReservaRequestDTO;
import portaria.dto.reserva.ReservaResponseDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.model.enums.StatusReserva;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.ReservaService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> solicitar(@RequestBody ReservaRequestDTO request) {
        UUID userId = currentUserId();
        var reserva = reservaService.solicitar(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponseDTO.fromEntity(reserva));
    }

    @GetMapping
    public List<ReservaResponseDTO> listar(@RequestParam(required = false) StatusReserva status) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (status != null) {
            return reservaService.listarPorCondominioEStatus(condominioId, status)
                    .stream().map(ReservaResponseDTO::fromEntity).toList();
        }
        return reservaService.listarPorCondominio(condominioId)
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/minhas")
    public List<ReservaResponseDTO> minhasReservas() {
        return reservaService.listarDoSolicitante(currentUserId())
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/area/{areaComunId}")
    public List<ReservaResponseDTO> listarPorArea(@PathVariable String areaComunId) {
        return reservaService.listarPorArea(areaComunId)
                .stream().map(ReservaResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponseDTO buscarPorId(@PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/aprovar")
    public ReservaResponseDTO aprovar(@PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.aprovar(id));
    }

    @PatchMapping("/{id}/recusar")
    public ReservaResponseDTO recusar(@PathVariable String id,
                                      @RequestBody RecusaReservaRequestDTO request) {
        return ReservaResponseDTO.fromEntity(reservaService.recusar(id, request.justificativa()));
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponseDTO cancelar(@PathVariable String id) {
        return ReservaResponseDTO.fromEntity(reservaService.cancelar(id));
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return UUID.fromString(claims.authUserId());
    }
}
