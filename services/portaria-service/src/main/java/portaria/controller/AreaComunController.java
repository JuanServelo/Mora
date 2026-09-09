package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.exception.AcessoNegadoException;
import portaria.model.AreaComum;
import portaria.security.CondominioUtils;
import portaria.service.AreaComunService;

import java.util.List;

@RestController
@RequestMapping("/areas-comuns")
@RequiredArgsConstructor
public class AreaComunController {

    private final AreaComunService areaComunService;

    @PostMapping("/cadastrar")
    public ResponseEntity<AreaComum> cadastrar(@Valid @RequestBody AreaComum areaComum) {
        String condominioIdEfetivo = CondominioUtils.condominioIdEfetivo();
        if (condominioIdEfetivo != null && !condominioIdEfetivo.equals(areaComum.getCondominioId())) {
            throw new AcessoNegadoException("Acesso negado ao condomínio especificado.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(areaComunService.cadastrar(areaComum));
    }

    @GetMapping
    public List<AreaComum> listarAtivas(@RequestParam(required = false) String condominioId) {
        return areaComunService.listarAtivas(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/todas")
    public List<AreaComum> listarTodas(@RequestParam(required = false) String condominioId) {
        return areaComunService.listarTodas(CondominioUtils.resolverEscopo(condominioId));
    }

    @GetMapping("/{id}")
    public AreaComum buscarPorId(@PathVariable String id) {
        return areaComunService.buscarPorId(id);
    }

    @GetMapping("/tipo/{tipo}")
    public List<AreaComum> listarPorTipo(@PathVariable String tipo) {
        return areaComunService.listarPorTipo(tipo);
    }

    @GetMapping("/tipo/{tipo}/ativas")
    public List<AreaComum> listarPorTipoAtivas(@PathVariable String tipo) {
        return areaComunService.listarPorTipoAtivas(tipo);
    }

    @PutMapping("/{id}")
    public AreaComum atualizar(@PathVariable String id, @Valid @RequestBody AreaComum dados) {
        String condominioIdEfetivo = CondominioUtils.condominioIdEfetivo();
        if (condominioIdEfetivo != null) {
            AreaComum existente = areaComunService.buscarPorId(id);
            if (!condominioIdEfetivo.equals(existente.getCondominioId())) {
                throw new AcessoNegadoException("Acesso negado ao condomínio especificado.");
            }
        }
        return areaComunService.atualizar(id, dados);
    }

    @PutMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        areaComunService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable String id) {
        areaComunService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        areaComunService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
