package comunicacao.repository;

import comunicacao.model.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvisoRepository extends JpaRepository<Aviso, UUID> {

    List<Aviso> findByCondominioIdOrderByCriadoEmDesc(String condominioId);

    List<Aviso> findAllByOrderByCriadoEmDesc();

    @Query("""
        SELECT a FROM Aviso a
        WHERE a.condominioId = :condominioId
          AND a.publicado = true
          AND a.dataInicio <= :hoje
          AND a.dataFim >= :hoje
        ORDER BY a.criadoEm DESC
        """)
    List<Aviso> findAtivos(@Param("condominioId") String condominioId,
                           @Param("hoje") LocalDate hoje);

    @Query("""
        SELECT a FROM Aviso a
        WHERE a.publicado = true
          AND a.dataInicio <= :hoje
          AND a.dataFim >= :hoje
        ORDER BY a.criadoEm DESC
        """)
    List<Aviso> findTodosAtivos(@Param("hoje") LocalDate hoje);
}
