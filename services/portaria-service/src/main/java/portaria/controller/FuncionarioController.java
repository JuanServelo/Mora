package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Funcionario;
import portaria.security.CondominioUtils;
import portaria.service.FuncionarioService;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Funcionario> cadastrar(@Valid @RequestBody Funcionario funcionario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.cadastrar(funcionario));
    }

    @GetMapping
    public List<Funcionario> listarAtivos() {
        return funcionarioService.listarAtivos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/todos")
    public List<Funcionario> listarTodos() {
        return funcionarioService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    public Funcionario buscarPorId(@PathVariable String id) {
        return funcionarioService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable String id, @Valid @RequestBody Funcionario dados) {
        return funcionarioService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        funcionarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
