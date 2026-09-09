package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Turno;
import portaria.repository.TurnoRepository;
import portaria.security.AuthContext;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TurnoService {

    private final TurnoRepository turnoRepository;

    public Turno iniciar(Turno turno) {
        var claims = AuthContext.get();
        if (claims != null && claims.condominioId() != null && !"default".equals(claims.condominioId())) {
            turno.setCondominioId(claims.condominioId());
        }
        turno.getEntradas().add(LocalDateTime.now());
        return turnoRepository.save(turno);
    }

    public Turno retomar(String id) {
        Turno turno = buscarPorId(id);
        if (turno.getEntradas().size() > turno.getSaidas().size()) {
            throw new OperacaoInvalidaException("Turno já está em andamento.");
        }
        turno.getEntradas().add(LocalDateTime.now());
        return turnoRepository.save(turno);
    }

    public Turno finalizar(String id) {
        Turno turno = buscarPorId(id);
        if (turno.getEntradas().size() <= turno.getSaidas().size()) {
            throw new OperacaoInvalidaException("Turno já está finalizado.");
        }
        turno.getSaidas().add(LocalDateTime.now());
        return turnoRepository.save(turno);
    }

    public List<Turno> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return turnoRepository.findByCondominioId(condominioId);
        }
        return turnoRepository.findAll();
    }

    public Turno buscarPorId(String id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turno não encontrado com id: " + id));
    }
}
