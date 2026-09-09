package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.PreAutorizacao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PreAutorizacaoRepository extends JpaRepository<PreAutorizacao, String> {

    List<PreAutorizacao> findByMoradorId(UUID moradorId);

    List<PreAutorizacao> findByCondominioIdAndAtivoTrue(String condominioId);

    List<PreAutorizacao> findByAtivoTrue();

    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE p.condominioId = :condominioId
          AND p.ativo = true
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
        """)
    List<PreAutorizacao> findAtivasHoje(@Param("condominioId") String condominioId,
                                        @Param("hoje") LocalDate hoje);

    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE p.ativo = true
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
        """)
    List<PreAutorizacao> findTodasAtivasHoje(@Param("hoje") LocalDate hoje);

    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE p.condominioId = :condominioId
          AND p.ativo = true
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
          AND (LOWER(p.nomeVisitante) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR p.cpfVisitante = :termo)
        """)
    List<PreAutorizacao> buscarPorNomeOuCpf(@Param("condominioId") String condominioId,
                                             @Param("hoje") LocalDate hoje,
                                             @Param("termo") String termo);
}
