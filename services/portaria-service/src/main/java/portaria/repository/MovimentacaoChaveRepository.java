package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import portaria.model.MovimentacaoChave;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentacaoChaveRepository extends JpaRepository<MovimentacaoChave, String> {

    Optional<MovimentacaoChave> findByChaveIdAndDataDevolucaoIsNull(String chaveId);

    // RN-09: sobrepõe o intervalo filtrado. Em aberto sempre no topo.
    @Query("""
        SELECT m FROM MovimentacaoChave m
        WHERE m.chaveId = :chaveId
          AND m.dataRetirada <= :filtroFim
          AND (m.dataDevolucao IS NULL OR m.dataDevolucao >= :filtroInicio)
          AND (:quemRetirouPattern IS NULL OR LOWER(m.nomeResponsavel) LIKE :quemRetirouPattern)
          AND (:perfil IS NULL OR m.perfilResponsavel = :perfil)
          AND (
            :status IS NULL
            OR (:status = 'EM_ABERTO'  AND m.dataDevolucao IS NULL)
            OR (:status = 'DEVOLVIDA'  AND m.dataDevolucao IS NOT NULL)
          )
        ORDER BY
          CASE WHEN m.dataDevolucao IS NULL THEN 0 ELSE 1 END ASC,
          m.dataRetirada DESC
        """)
    List<MovimentacaoChave> buscarHistoricoFiltrado(
        @Param("chaveId") String chaveId,
        @Param("filtroInicio") LocalDateTime filtroInicio,
        @Param("filtroFim") LocalDateTime filtroFim,
        @Param("quemRetirouPattern") String quemRetirouPattern,
        @Param("perfil") String perfil,
        @Param("status") String status
    );
}
