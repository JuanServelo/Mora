package comunicacao.controller;

import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.security.CondominioUtils;
import comunicacao.service.ArtigoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/artigos")
@RequiredArgsConstructor
public class ArtigoController {

    private final ArtigoService artigoService;

    @PostMapping
    public ResponseEntity<ArtigoConhecimento> criar(@RequestBody ArtigoConhecimento artigo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artigoService.criar(artigo));
    }

    @GetMapping
    public List<ArtigoConhecimento> listar(
            @RequestParam(required = false) boolean publicadosOnly,
            @RequestParam(required = false) CategoriaArtigo categoria) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (publicadosOnly || categoria != null) {
            return artigoService.listarPublicados(condominioId, categoria);
        }
        return artigoService.listarTodos(condominioId);
    }

    @GetMapping("/{id}")
    public ArtigoConhecimento buscarPorId(@PathVariable UUID id) {
        return artigoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ArtigoConhecimento atualizar(@PathVariable UUID id, @RequestBody ArtigoConhecimento dados) {
        return artigoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/publicar")
    public ArtigoConhecimento publicar(@PathVariable UUID id) {
        return artigoService.publicar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        artigoService.excluir(id);
    }
}
