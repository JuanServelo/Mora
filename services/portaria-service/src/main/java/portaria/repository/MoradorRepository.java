package portaria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.Morador;
import java.util.List;
import java.util.Optional;

public interface MoradorRepository extends JpaRepository<Morador, String> {
    Optional<Morador> findByCpf(String cpf);
    Optional<Morador> findByEmail(String email);
    List<Morador> findByAtivo(boolean ativo);
    List<Morador> findByCondominioId(String condominioId);
    Page<Morador> findByCondominioId(String condominioId, Pageable pageable);
    List<Morador> findByCondominioIdAndAtivo(String condominioId, boolean ativo);
    Page<Morador> findByCondominioIdAndAtivo(String condominioId, boolean ativo, Pageable pageable);
    Page<Morador> findByAtivo(boolean ativo, Pageable pageable);

    @Query("SELECT m FROM Morador m LEFT JOIN FETCH m.apartamento WHERE m.id = :id")
    Optional<Morador> findByIdComApartamento(@Param("id") String id);

    boolean existsByApartamento_IdAndAtivo(java.util.UUID apartamentoId, boolean ativo);
}
