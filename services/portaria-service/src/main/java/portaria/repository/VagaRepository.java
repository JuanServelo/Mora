package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.Vaga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VagaRepository extends JpaRepository<Vaga, String> {
    Optional<Vaga> findByNumero(String numero);
    List<Vaga> findByApartamentoId(UUID apartamentoId);
    List<Vaga> findByAtiva(boolean ativa);
    List<Vaga> findByCondominioId(String condominioId);
    List<Vaga> findByCondominioIdAndAtiva(String condominioId, boolean ativa);
}
