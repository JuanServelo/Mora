package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.Apartamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApartamentoRepository extends JpaRepository<Apartamento, UUID> {
    List<Apartamento> findByAtivo(boolean ativo);
    List<Apartamento> findByBloco_Id(UUID blocoId);
    List<Apartamento> findByBloco_IdAndAtivo(UUID blocoId, boolean ativo);
    Optional<Apartamento> findByNumeroAndBloco_Id(String numero, UUID blocoId);

    List<Apartamento> findByCondominioId(String condominioId);
    List<Apartamento> findByCondominioIdAndAtivo(String condominioId, boolean ativo);
    long countByBloco_IdAndAndar(UUID blocoId, int andar);
}
