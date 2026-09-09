package comunicacao.repository;

import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtigoConhecimentoRepository extends JpaRepository<ArtigoConhecimento, UUID> {

    List<ArtigoConhecimento> findByCategoria(CategoriaArtigo categoria);

    List<ArtigoConhecimento> findByPublicadoTrue();

    List<ArtigoConhecimento> findByCondominioId(String condominioId);

    List<ArtigoConhecimento> findByCondominioIdAndPublicadoTrue(String condominioId);

    List<ArtigoConhecimento> findByCategoriaAndPublicadoTrue(CategoriaArtigo categoria);
}
