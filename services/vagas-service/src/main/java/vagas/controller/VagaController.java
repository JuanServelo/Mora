package vagas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vagas.dto.VagaRequestDTO;
import vagas.dto.VagaResponseDTO;
import vagas.exception.OperacaoInvalidaException;
import vagas.model.Vaga;
import vagas.security.AuthContext;
import vagas.security.JwtClaims;
import vagas.service.VagaService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vagas")
@RequiredArgsConstructor
public class VagaController {

    private final VagaService vagaService;

    @PostMapping("/cadastrar")
    public ResponseEntity<VagaResponseDTO> cadastrar(@Valid @RequestBody VagaRequestDTO dto) {
        validarAdministrador();
        Vaga salvo = vagaService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(VagaResponseDTO.fromEntity(salvo));
    }

    @GetMapping
    public List<VagaResponseDTO> listarApenasAtivas() {
        return vagaService.listarApenasAtivas().stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/todas")
    public List<VagaResponseDTO> listarTodas() {
        validarAdministrador();
        return vagaService.listarTodas().stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public VagaResponseDTO buscarPorId(@PathVariable String id) {
        return VagaResponseDTO.fromEntity(vagaService.buscarPorId(id));
    }

    @GetMapping("/apartamento/{apartamentoId}")
    public List<VagaResponseDTO> buscarPorApartamento(@PathVariable UUID apartamentoId) {
        return vagaService.buscarPorApartamento(apartamentoId).stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @PutMapping("/{id}")
    public VagaResponseDTO atualizar(@PathVariable String id, @Valid @RequestBody VagaRequestDTO dto) {
        validarAdministrador();
        return VagaResponseDTO.fromEntity(vagaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        validarAdministrador();
        vagaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable String id) {
        validarAdministrador();
        vagaService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    private void validarAdministrador() {
        JwtClaims claims = AuthContext.get();
        if (claims == null) {
            throw new OperacaoInvalidaException("Acesso negado: autenticação necessária.");
        }
        String perfil = claims.perfil();
        if (perfil == null || !(perfil.equals("ADMIN_SINDICO") || perfil.equals("ADMIN_GERAL"))) {
            throw new OperacaoInvalidaException(
                    "Acesso negado: apenas administradores podem executar esta operação.");
        }
    }
}
