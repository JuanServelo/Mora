package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Bloco;
import portaria.repository.ApartamentoRepository;
import portaria.repository.BlocoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BlocoService {

    private static final int MAX_ANDARES = 163;
    private static final int MAX_APTOS_POR_ANDAR = 30;

    private final BlocoRepository blocoRepository;
    private final ApartamentoRepository apartamentoRepository;

    public Bloco cadastrar(Bloco bloco) {
        validarLimitesBloco(bloco.getAndares(), bloco.getApartamentosPorAndar());

        if (bloco.getCondominioId() != null) {
            blocoRepository.findByNomeAndCondominioId(bloco.getNome(), bloco.getCondominioId()).ifPresent(b -> {
                throw new OperacaoInvalidaException("Já existe um bloco com o nome '" + bloco.getNome() + "' neste cliente.");
            });
        } else {
            blocoRepository.findByNome(bloco.getNome()).ifPresent(b -> {
                throw new OperacaoInvalidaException("Já existe um bloco cadastrado com o nome: " + bloco.getNome());
            });
        }
        return blocoRepository.save(bloco);
    }

    public List<Bloco> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return blocoRepository.findByCondominioId(condominioId);
        }
        return blocoRepository.findAll();
    }

    public List<Bloco> listarAtivos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return blocoRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return blocoRepository.findByAtivo(true);
    }

    public Bloco buscarPorId(UUID id) {
        return blocoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado com id: " + id));
    }

    public Bloco atualizar(UUID id, Bloco dados) {
        Bloco bloco = buscarPorId(id);
        String condId = bloco.getCondominioId();

        validarLimitesBloco(dados.getAndares(), dados.getApartamentosPorAndar());

        // RN-05: não reduzir abaixo do que já existe
        var apts = apartamentoRepository.findByBloco_Id(id);
        if (dados.getApartamentosPorAndar() != null && !apts.isEmpty()) {
            Map<Integer, Long> countByAndar = apts.stream()
                .filter(a -> a.getAndar() != null)
                .collect(Collectors.groupingBy(a -> a.getAndar(), Collectors.counting()));
            long maxCount = countByAndar.values().stream().mapToLong(Long::longValue).max().orElse(0);
            if (dados.getApartamentosPorAndar() < maxCount) {
                throw new OperacaoInvalidaException(
                    "Não é possível reduzir para " + dados.getApartamentosPorAndar() +
                    " apartamentos/andar pois já existe um andar com " + maxCount + " apartamentos cadastrados.");
            }
        }
        if (dados.getAndares() != null && !apts.isEmpty()) {
            int maxAndar = apts.stream()
                .filter(a -> a.getAndar() != null)
                .mapToInt(a -> a.getAndar())
                .max().orElse(0);
            if (dados.getAndares() < maxAndar) {
                throw new OperacaoInvalidaException(
                    "Não é possível reduzir para " + dados.getAndares() +
                    " andares pois já existe um apartamento no " + maxAndar + "º andar.");
            }
        }

        if (!bloco.getNome().equals(dados.getNome())) {
            if (condId != null) {
                blocoRepository.findByNomeAndCondominioId(dados.getNome(), condId).ifPresent(b -> {
                    throw new OperacaoInvalidaException("Já existe um bloco com o nome: " + dados.getNome());
                });
            } else {
                blocoRepository.findByNome(dados.getNome()).ifPresent(b -> {
                    throw new OperacaoInvalidaException("Já existe um bloco com o nome: " + dados.getNome());
                });
            }
            bloco.setNome(dados.getNome());
        }

        bloco.setDescricao(dados.getDescricao());
        bloco.setAndares(dados.getAndares());
        bloco.setApartamentosPorAndar(dados.getApartamentosPorAndar());
        bloco.setAtualizadoEm(LocalDateTime.now());

        return blocoRepository.save(bloco);
    }

    public void desativar(UUID id) {
        Bloco bloco = buscarPorId(id);
        bloco.setAtivo(false);
        bloco.setAtualizadoEm(LocalDateTime.now());
        blocoRepository.save(bloco);
    }

    public void ativar(UUID id) {
        Bloco bloco = buscarPorId(id);
        bloco.setAtivo(true);
        bloco.setAtualizadoEm(LocalDateTime.now());
        blocoRepository.save(bloco);
    }

    public void deletar(UUID id) {
        if (!blocoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Bloco não encontrado com id: " + id);
        }
        blocoRepository.deleteById(id);
    }

    private void validarLimitesBloco(Integer andares, Integer apartamentosPorAndar) {
        if (andares != null) {
            if (andares < 1) {
                throw new OperacaoInvalidaException("Informe um número inteiro maior que zero.");
            }
            if (andares > MAX_ANDARES) {
                throw new OperacaoInvalidaException("O número máximo de andares permitido é " + MAX_ANDARES + ".");
            }
        }
        if (apartamentosPorAndar != null) {
            if (apartamentosPorAndar < 1) {
                throw new OperacaoInvalidaException("Informe um número inteiro maior que zero.");
            }
            if (apartamentosPorAndar > MAX_APTOS_POR_ANDAR) {
                throw new OperacaoInvalidaException("O número máximo de apartamentos por andar é " + MAX_APTOS_POR_ANDAR + ".");
            }
        }
    }
}
