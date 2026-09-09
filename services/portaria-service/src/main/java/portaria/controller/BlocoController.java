package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Bloco;
import portaria.security.CondominioUtils;
import portaria.service.BlocoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/blocos")
@RequiredArgsConstructor
public class BlocoController {

    private final BlocoService blocoService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Bloco> cadastrar(@Valid @RequestBody Bloco bloco) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blocoService.cadastrar(bloco));
    }

    @GetMapping
    public List<Bloco> listarAtivos(@RequestParam(required = false) String condominioId) {
        return blocoService.listarAtivos(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/todos")
    public List<Bloco> listarTodos(@RequestParam(required = false) String condominioId) {
        return blocoService.listarTodos(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/{id}")
    public Bloco buscarPorId(@PathVariable UUID id) {
        return blocoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Bloco atualizar(@PathVariable UUID id, @Valid @RequestBody Bloco dados) {
        return blocoService.atualizar(id, dados);
    }

    @PutMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        blocoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        blocoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        blocoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
