package portaria.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.PreAutorizacaoRequestDTO;
import portaria.dto.PreAutorizacaoResponseDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.service.PreAutorizacaoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pre-autorizacoes")
@RequiredArgsConstructor
public class PreAutorizacaoController {

    private final PreAutorizacaoService preAutorizacaoService;

    @PostMapping
    public ResponseEntity<PreAutorizacaoResponseDTO> cadastrar(@RequestBody PreAutorizacaoRequestDTO request) {
        var pa = preAutorizacaoService.cadastrar(currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PreAutorizacaoResponseDTO.fromEntity(pa));
    }

    @GetMapping("/hoje")
    public List<PreAutorizacaoResponseDTO> listarAtivasHoje() {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.listarAtivasHoje(condominioId)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/buscar")
    public List<PreAutorizacaoResponseDTO> buscar(@RequestParam String termo) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return preAutorizacaoService.buscarPorNomeOuCpf(condominioId, termo)
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/minhas")
    public List<PreAutorizacaoResponseDTO> minhas() {
        return preAutorizacaoService.listarDoMorador(currentUserId())
                .stream().map(PreAutorizacaoResponseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public PreAutorizacaoResponseDTO buscarPorId(@PathVariable String id) {
        return PreAutorizacaoResponseDTO.fromEntity(preAutorizacaoService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogar(@PathVariable String id) {
        preAutorizacaoService.revogar(id);
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return UUID.fromString(claims.authUserId());
    }
}
