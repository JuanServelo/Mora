package comunicacao.controller;

import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.model.ChatMensagem;
import comunicacao.security.AuthContext;
import comunicacao.security.JwtClaims;
import comunicacao.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/mensagem")
    public ResponseEntity<ChatMensagem> enviar(
            @RequestParam UUID destinatarioId,
            @RequestBody String texto) {
        ChatMensagem msg = chatService.enviar(currentUserId(), destinatarioId, texto);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @GetMapping("/conversa/{outroUsuarioId}")
    public List<ChatMensagem> conversa(@PathVariable UUID outroUsuarioId) {
        return chatService.buscarConversa(currentUserId(), outroUsuarioId);
    }

    @PatchMapping("/conversa/{outroUsuarioId}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarConversaLida(@PathVariable UUID outroUsuarioId) {
        chatService.marcarConversaLida(currentUserId(), outroUsuarioId);
    }

    @GetMapping("/nao-lidas")
    public List<ChatMensagem> listarNaoLidas() {
        return chatService.listarNaoLidas(currentUserId());
    }

    @GetMapping("/contador")
    public Map<String, Long> contador() {
        return Map.of("naoLidas", chatService.contarNaoLidas(currentUserId()));
    }

    @PatchMapping("/mensagem/{id}/lida")
    public ChatMensagem marcarLida(@PathVariable String id) {
        return chatService.marcarLida(id, currentUserId());
    }

    private UUID currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return UUID.fromString(claims.authUserId());
    }
}
