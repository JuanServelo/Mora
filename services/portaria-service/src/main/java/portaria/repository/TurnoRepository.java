package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Turno;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, String> {
    List<Turno> findByFuncionario(String funcionario);
    List<Turno> findByCondominioId(String condominioId);
}
