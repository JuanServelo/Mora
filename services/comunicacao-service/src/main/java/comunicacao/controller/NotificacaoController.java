package comunicacao.controller;

import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.security.AuthContext;
import comunicacao.security.JwtClaims;
import comunicacao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    public Page<Notificacao> listar(@PageableDefault(size = 20) Pageable pageable) {
        return notificacaoService.listar(currentUserId(), pageable);
    }

    @GetMapping("/nao-lidas")
    public List<Notificacao> listarNaoLidas() {
        return notificacaoService.listarNaoLidas(currentUserId());
    }

    @GetMapping("/contador")
    public Map<String, Long> contador() {
        return Map.of("naoLidas", notificacaoService.contarNaoLidas(currentUserId()));
    }

    @PatchMapping("/{id}/lida")
    public Notificacao marcarLida(@PathVariable String id) {
        return notificacaoService.marcarLida(id);
    }

    @PatchMapping("/todas-lidas")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void marcarTodasLidas() {
        notificacaoService.marcarTodasLidas(currentUserId());
    }

    @PostMapping("/admin")
    public ResponseEntity<Notificacao> criarAdmin(
            @RequestParam UUID destinatarioId,
            @RequestParam String condominioId,
            @RequestParam TipoNotificacao tipo,
            @RequestParam String titulo,
            @RequestBody String mensagem) {
        return ResponseEntity.ok(notificacaoService.criar(
                destinatarioId, condominioId, tipo, titulo, mensagem, null));
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return UUID.fromString(claims.authUserId());
    }
}
