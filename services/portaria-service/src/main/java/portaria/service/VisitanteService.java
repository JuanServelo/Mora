package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import portaria.repository.VisitanteRepository;
import portaria.security.AuthContext;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;

    public Visitante registrarEntrada(Visitante visitante) {
        var claims = AuthContext.get();
        if (claims != null && claims.condominioId() != null && !"default".equals(claims.condominioId())) {
            visitante.setCondominioId(claims.condominioId());
        }
        visitante.setHorarioEntrada(LocalDateTime.now());
        visitante.setStatus(StatusAcesso.DENTRO);
        return visitanteRepository.save(visitante);
    }

    public Visitante registrarSaida(String id) {
        Visitante visitante = buscarPorId(id);
        if (visitante.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Visitante não está registrado como DENTRO do condomínio.");
        }
        visitante.setHorarioSaida(LocalDateTime.now());
        visitante.setStatus(StatusAcesso.SAIU);
        return visitanteRepository.save(visitante);
    }

    public List<Visitante> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return visitanteRepository.findByCondominioId(condominioId);
        }
        return visitanteRepository.findAll();
    }

    public List<Visitante> listarDentro(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return visitanteRepository.findByCondominioIdAndStatus(condominioId, StatusAcesso.DENTRO);
        }
        return visitanteRepository.findByStatus(StatusAcesso.DENTRO);
    }

    public Visitante buscarPorId(String id) {
        return visitanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Visitante não encontrado com id: " + id));
    }
}
