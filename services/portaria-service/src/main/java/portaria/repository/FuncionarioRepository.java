package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Funcionario;
import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, String> {
    Optional<Funcionario> findByCpf(String cpf);
    List<Funcionario> findByAtivo(boolean ativo);
    List<Funcionario> findByCondominioId(String condominioId);
    List<Funcionario> findByCondominioIdAndAtivo(String condominioId, boolean ativo);
}
