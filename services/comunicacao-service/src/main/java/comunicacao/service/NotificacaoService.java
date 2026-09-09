package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Notificacao;
import comunicacao.model.enums.TipoNotificacao;
import comunicacao.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public Notificacao criar(UUID destinatarioId, String condominioId,
                             TipoNotificacao tipo, String titulo, String mensagem,
                             String referenciaId) {
        Notificacao n = new Notificacao();
        n.setDestinatarioId(destinatarioId);
        n.setCondominioId(condominioId);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setReferenciaId(referenciaId);
        return notificacaoRepository.save(n);
    }

    public Notificacao marcarLida(String id) {
        Notificacao n = buscarPorId(id);
        if (!n.isLida()) {
            n.setLida(true);
            n.setLidaEm(LocalDateTime.now());
            notificacaoRepository.save(n);
        }
        return n;
    }

    public void marcarTodasLidas(UUID destinatarioId) {
        notificacaoRepository
                .findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(destinatarioId)
                .forEach(n -> {
                    n.setLida(true);
                    n.setLidaEm(LocalDateTime.now());
                });
    }

    @Transactional(readOnly = true)
    public Notificacao buscarPorId(String id) {
        return notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(UUID destinatarioId, Pageable pageable) {
        return notificacaoRepository.findByDestinatarioIdOrderByCriadoEmDesc(destinatarioId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listarNaoLidas(UUID destinatarioId) {
        return notificacaoRepository.findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(destinatarioId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(UUID destinatarioId) {
        return notificacaoRepository.countByDestinatarioIdAndLidaFalse(destinatarioId);
    }
}
