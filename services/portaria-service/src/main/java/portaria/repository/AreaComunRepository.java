package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.AreaComum;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaComunRepository extends JpaRepository<AreaComum, String> {
    List<AreaComum> findByAtivo(boolean ativo);
    List<AreaComum> findByTipo(String tipo);
    Optional<AreaComum> findByNome(String nome);
    Optional<AreaComum> findByNomeIgnoreCase(String nome);

    List<AreaComum> findByCondominioId(String condominioId);
    List<AreaComum> findByCondominioIdAndAtivo(String condominioId, boolean ativo);
    Optional<AreaComum> findByNomeAndCondominioId(String nome, String condominioId);
    Optional<AreaComum> findByNomeIgnoreCaseAndCondominioId(String nome, String condominioId);
}
