package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Morador;
import portaria.repository.MoradorRepository;
import portaria.security.AuthContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MoradorService {

    private final MoradorRepository moradorRepository;

    public Morador cadastrar(Morador morador) {
        var claims = AuthContext.get();
        String condominioId = (claims != null && claims.condominioId() != null && !"default".equals(claims.condominioId()))
                ? claims.condominioId() : morador.getCondominioId();

        if (condominioId != null) {
            final String cid = condominioId;
            moradorRepository.findByCpf(morador.getCpf()).ifPresent(m -> {
                if (cid.equals(m.getCondominioId())) {
                    throw new OperacaoInvalidaException("Já existe um morador cadastrado com o CPF: " + morador.getCpf());
                }
            });
        } else {
            moradorRepository.findByCpf(morador.getCpf()).ifPresent(m -> {
                throw new OperacaoInvalidaException("Já existe um morador cadastrado com o CPF: " + morador.getCpf());
            });
        }
        morador.setCondominioId(condominioId);
        return moradorRepository.save(morador);
    }

    public List<Morador> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return moradorRepository.findByCondominioId(condominioId);
        }
        return moradorRepository.findAll();
    }

    public Page<Morador> listarTodosPaginado(String condominioId, Pageable pageable) {
        if (condominioId != null && !condominioId.isBlank()) {
            return moradorRepository.findByCondominioId(condominioId, pageable);
        }
        return moradorRepository.findAll(pageable);
    }

    public List<Morador> listarAtivos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return moradorRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return moradorRepository.findByAtivo(true);
    }

    public Page<Morador> listarAtivosPaginado(String condominioId, Pageable pageable) {
        if (condominioId != null && !condominioId.isBlank()) {
            return moradorRepository.findByCondominioIdAndAtivo(condominioId, true, pageable);
        }
        return moradorRepository.findByAtivo(true, pageable);
    }

    public Morador buscarPorId(String id) {
        return moradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado com id: " + id));
    }

    public Morador atualizar(String id, Morador dados) {
        Morador morador = buscarPorId(id);
        morador.setNome(dados.getNome());
        morador.setApartamento(dados.getApartamento());
        morador.setBloco(dados.getBloco());
        morador.setTelefone(dados.getTelefone());
        return moradorRepository.save(morador);
    }

    public void desativar(String id) {
        Morador morador = buscarPorId(id);
        morador.setAtivo(false);
        moradorRepository.save(morador);
    }
}
