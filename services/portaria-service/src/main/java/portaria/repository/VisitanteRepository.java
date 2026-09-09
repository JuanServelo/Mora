package portaria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoVisita;
import java.util.List;
import java.util.Optional;

public interface VisitanteRepository extends JpaRepository<Visitante, String> {
    List<Visitante> findByStatus(StatusAcesso status);
    Optional<Visitante> findByCpf(String cpf);
    Optional<Visitante> findByCondominioIdAndCpf(String condominioId, String cpf);
    List<Visitante> findByCondominioId(String condominioId);
    Page<Visitante> findByCondominioId(String condominioId, Pageable pageable);
    List<Visitante> findByCondominioIdAndStatus(String condominioId, StatusAcesso status);
    Page<Visitante> findByCondominioIdAndStatus(String condominioId, StatusAcesso status, Pageable pageable);
    Page<Visitante> findByStatus(StatusAcesso status, Pageable pageable);

    @Query("""
        SELECT v FROM Visitante v
        WHERE v.condominioId = :condominioId
          AND v.tipoVisita = :tipo
          AND (
            LOWER(v.nome) LIKE LOWER(CONCAT('%', :q, '%'))
            OR v.documento LIKE CONCAT('%', :q, '%')
          )
        ORDER BY v.nome
        """)
    Page<Visitante> buscarPorNomeOuDocumento(
            @Param("condominioId") String condominioId,
            @Param("tipo") TipoVisita tipo,
            @Param("q") String q,
            Pageable pageable);
}
