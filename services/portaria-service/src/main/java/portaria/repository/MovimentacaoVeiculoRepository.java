package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import portaria.model.MovimentacaoVeiculo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentacaoVeiculoRepository extends JpaRepository<MovimentacaoVeiculo, String> {

    Optional<MovimentacaoVeiculo> findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(
            String condominioId, String placa);

    List<MovimentacaoVeiculo> findByCondominioIdAndSaidaEmIsNullOrderByEntradaEmDesc(
            String condominioId);

    /** Verifica se uma vaga está ocupada por algum veículo em aberto. */
    boolean existsByCondominioIdAndVagaIdAndSaidaEmIsNull(String condominioId, String vagaId);

    /** Retorna a movimentação em aberto de uma vaga (para exibir placa e horário). */
    Optional<MovimentacaoVeiculo> findFirstByCondominioIdAndVagaIdAndSaidaEmIsNull(String condominioId, String vagaId);

    // Histórico com overlap de período. Registros em aberto fixados no topo.
    @Query("""
        SELECT m FROM MovimentacaoVeiculo m
        WHERE m.condominioId = :condominioId
          AND m.entradaEm <= :filtroFim
          AND (m.saidaEm IS NULL OR m.saidaEm >= :filtroInicio)
          AND (:placaPattern IS NULL OR UPPER(m.placa) LIKE :placaPattern)
          AND (
            :status IS NULL
            OR (:status = 'DENTRO' AND m.saidaEm IS NULL)
            OR (:status = 'SAIU'   AND m.saidaEm IS NOT NULL)
          )
        ORDER BY
          CASE WHEN m.saidaEm IS NULL THEN 0 ELSE 1 END ASC,
          m.entradaEm DESC
        """)
    List<MovimentacaoVeiculo> buscarHistoricoFiltrado(
            @Param("condominioId") String condominioId,
            @Param("filtroInicio") LocalDateTime filtroInicio,
            @Param("filtroFim") LocalDateTime filtroFim,
            @Param("placaPattern") String placaPattern,
            @Param("status") String status
    );
}
