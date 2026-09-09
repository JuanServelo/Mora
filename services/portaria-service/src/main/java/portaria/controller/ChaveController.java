package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.chave.*;
import portaria.security.CondominioUtils;
import portaria.service.ChaveService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/chaves")
@RequiredArgsConstructor
public class ChaveController {

    private final ChaveService chaveService;

    @GetMapping("/locais")
    public List<LocalChaveDTO> listarLocais() {
        return chaveService.listarLocaisElegiveis(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping
    public List<ChaveResponseDTO> listarTodas() {
        return chaveService.listarTodas(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    public ChaveResponseDTO buscarPorId(@PathVariable String id) {
        return chaveService.buscarPorId(id);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<ChaveResponseDTO> cadastrar(@Valid @RequestBody CadastrarChaveDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chaveService.cadastrar(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        chaveService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retirar")
    public ChaveResponseDTO retirar(
        @PathVariable String id,
        @Valid @RequestBody RetirarChaveRequest request
    ) {
        return chaveService.retirar(id, request);
    }

    @PostMapping("/{id}/devolver")
    public ChaveResponseDTO devolver(@PathVariable String id) {
        return chaveService.devolver(id);
    }

    @GetMapping("/{id}/historico")
    public List<MovimentacaoChaveResponseDTO> historico(
        @PathVariable String id,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        @RequestParam(required = false) String quemRetirou,
        @RequestParam(required = false) String perfil,
        @RequestParam(required = false) String status
    ) {
        return chaveService.buscarHistorico(id, dataInicio, dataFim, quemRetirou, perfil, status);
    }
}
