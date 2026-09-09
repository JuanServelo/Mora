package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Morador;
import portaria.security.CondominioUtils;
import portaria.service.MoradorService;

import java.util.List;

@RestController
@RequestMapping("/moradores")
@RequiredArgsConstructor
public class MoradorController {

    private final MoradorService moradorService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Morador> cadastrar(@Valid @RequestBody Morador morador) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moradorService.cadastrar(morador));
    }

    @GetMapping
    public List<Morador> listarAtivos() {
        return moradorService.listarAtivos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/paginado")
    public Page<Morador> listarAtivosPaginado(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return moradorService.listarAtivosPaginado(CondominioUtils.condominioIdEfetivo(), pageable);
    }

    @GetMapping("/todos")
    public List<Morador> listarTodos() {
        return moradorService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/todos/paginado")
    public Page<Morador> listarTodosPaginado(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return moradorService.listarTodosPaginado(CondominioUtils.condominioIdEfetivo(), pageable);
    }

    @GetMapping("/{id}")
    public Morador buscarPorId(@PathVariable String id) {
        return moradorService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Morador atualizar(@PathVariable String id, @Valid @RequestBody Morador dados) {
        return moradorService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        moradorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
