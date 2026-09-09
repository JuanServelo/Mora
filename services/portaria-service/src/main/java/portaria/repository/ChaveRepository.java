package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Chave;

import java.util.List;
import java.util.Optional;

public interface ChaveRepository extends JpaRepository<Chave, String> {
    List<Chave> findByCondominioId(String condominioId);
    Optional<Chave> findByLocalIdAndNomeNormalizado(String localId, String nomeNormalizado);
}
