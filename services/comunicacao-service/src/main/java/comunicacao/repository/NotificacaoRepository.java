package comunicacao.repository;

import comunicacao.model.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {

    List<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(UUID destinatarioId);

    Page<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(UUID destinatarioId, Pageable pageable);

    List<Notificacao> findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(UUID destinatarioId);

    long countByDestinatarioIdAndLidaFalse(UUID destinatarioId);
}
