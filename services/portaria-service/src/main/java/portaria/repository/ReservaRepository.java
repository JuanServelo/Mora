package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.Reserva;
import portaria.model.enums.StatusReserva;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, String> {

    List<Reserva> findBySolicitanteId(UUID solicitanteId);

    List<Reserva> findBySolicitanteIdAndStatus(UUID solicitanteId, StatusReserva status);

    List<Reserva> findByCondominioId(String condominioId);

    List<Reserva> findByCondominioIdAndStatus(String condominioId, StatusReserva status);

    List<Reserva> findByAreaComum_Id(String areaComunId);

    List<Reserva> findByAreaComum_IdAndStatus(String areaComunId, StatusReserva status);

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.areaComum.id = :areaComunId
          AND r.status IN (portaria.model.enums.StatusReserva.PENDENTE, portaria.model.enums.StatusReserva.APROVADA)
          AND r.dataInicio <= :dataFim
          AND r.dataFim   >= :dataInicio
        """)
    List<Reserva> findConflitantes(@Param("areaComunId") String areaComunId,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim);
}
