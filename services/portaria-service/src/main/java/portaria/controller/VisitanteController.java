package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Visitante;
import portaria.security.CondominioUtils;
import portaria.service.VisitanteService;

import java.util.List;

@RestController
@RequestMapping("/visitantes")
@RequiredArgsConstructor
public class VisitanteController {

    private final VisitanteService visitanteService;

    @PostMapping("/entrada")
    public ResponseEntity<Visitante> registrarEntrada(@Valid @RequestBody Visitante visitante) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitanteService.registrarEntrada(visitante));
    }

    @PostMapping("/{id}/saida")
    public Visitante registrarSaida(@PathVariable String id) {
        return visitanteService.registrarSaida(id);
    }

    @GetMapping
    public List<Visitante> listarTodos() {
        return visitanteService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/dentro")
    public List<Visitante> listarDentro() {
        return visitanteService.listarDentro(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    public Visitante buscarPorId(@PathVariable String id) {
        return visitanteService.buscarPorId(id);
    }
}
