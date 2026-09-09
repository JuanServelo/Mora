package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.AluguelVaga;
import portaria.model.enums.StatusAluguel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AluguelVagaRepository extends JpaRepository<AluguelVaga, String> {

    List<AluguelVaga> findBySolicitanteId(UUID solicitanteId);

    List<AluguelVaga> findByProprietarioId(UUID proprietarioId);

    List<AluguelVaga> findBySolicitanteIdAndStatus(UUID solicitanteId, StatusAluguel status);

    List<AluguelVaga> findByProprietarioIdAndStatus(UUID proprietarioId, StatusAluguel status);

    List<AluguelVaga> findByCondominioId(String condominioId);

    List<AluguelVaga> findByCondominioIdAndStatus(String condominioId, StatusAluguel status);

    @Query("""
            SELECT a FROM AluguelVaga a
            WHERE a.vaga.id = :vagaId
              AND a.status = portaria.model.enums.StatusAluguel.APROVADO
              AND a.dataInicio <= :dataFim
              AND a.dataFim   >= :dataInicio
            """)
    List<AluguelVaga> findAprovadosConflitantes(
            @Param("vagaId") String vagaId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);
}
