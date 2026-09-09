package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Entrega;
import java.util.List;
import java.util.UUID;

public interface EntregaRepository extends JpaRepository<Entrega, String> {
    List<Entrega> findByStatus(String status);
    List<Entrega> findByDestinatarioId(Long destinatarioId);
    List<Entrega> findByBloco(String bloco);
    List<Entrega> findByApartamento(String apartamento);
    List<Entrega> findByCondominioId(String condominioId);
    List<Entrega> findByCondominioIdAndStatus(String condominioId, String status);
    List<Entrega> findByMoradorId(UUID moradorId);
    List<Entrega> findByMoradorIdAndStatus(UUID moradorId, String status);
}
