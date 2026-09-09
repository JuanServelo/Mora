package comunicacao.repository;

import comunicacao.model.AvisoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvisoLeituraRepository extends JpaRepository<AvisoLeitura, String> {

    Optional<AvisoLeitura> findByAvisoIdAndUsuarioId(UUID avisoId, UUID usuarioId);

    List<AvisoLeitura> findByUsuarioId(UUID usuarioId);

    long countByAvisoId(UUID avisoId);

    @Query("SELECT al.avisoId FROM AvisoLeitura al WHERE al.usuarioId = :usuarioId")
    List<UUID> findAvisosLidosByUsuario(@Param("usuarioId") UUID usuarioId);
}
