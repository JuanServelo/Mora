package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Turno;
import portaria.security.CondominioUtils;
import portaria.service.TurnoService;

import java.util.List;

@RestController
@RequestMapping("/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping("/iniciar")
    public ResponseEntity<Turno> iniciar(@Valid @RequestBody Turno turno) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.iniciar(turno));
    }

    @PostMapping("/{id}/retomar")
    public Turno retomar(@PathVariable String id) {
        return turnoService.retomar(id);
    }

    @PostMapping("/{id}/finalizar")
    public Turno finalizar(@PathVariable String id) {
        return turnoService.finalizar(id);
    }

    @GetMapping
    public List<Turno> listarTodos() {
        return turnoService.listarTodos(CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    public Turno buscarPorId(@PathVariable String id) {
        return turnoService.buscarPorId(id);
    }
}
