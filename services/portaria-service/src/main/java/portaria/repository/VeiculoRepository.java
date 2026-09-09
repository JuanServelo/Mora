package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.Veiculo;
import portaria.model.enums.StatusAcesso;
import java.util.List;
import java.util.Optional;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, String> {
    Optional<Veiculo> findByPlaca(String placa);
    Optional<Veiculo> findByPlacaAndCondominioId(String placa, String condominioId);
    List<Veiculo> findByStatus(StatusAcesso status);
    List<Veiculo> findByProprietarioId(String proprietarioId);
    List<Veiculo> findByVagaId(String vagaId);
    List<Veiculo> findByCondominioId(String condominioId);
    List<Veiculo> findByCondominioIdAndStatus(String condominioId, StatusAcesso status);
    List<Veiculo> findByCondominioIdAndProprietarioId(String condominioId, String proprietarioId);
    Optional<Veiculo> findFirstByProprietarioIdAndStatus(String proprietarioId, StatusAcesso status);
}
