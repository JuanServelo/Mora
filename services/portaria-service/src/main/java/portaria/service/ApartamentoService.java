package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.ApartamentoRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Apartamento;
import portaria.model.Bloco;
import portaria.repository.ApartamentoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApartamentoService {

    private static final int MIN_QUARTOS = 1;
    private static final int MAX_QUARTOS = 10;
    private static final double MIN_AREA_M2 = 10.0;
    private static final double MAX_AREA_M2 = 2000.0;

    private final ApartamentoRepository apartamentoRepository;
    private final BlocoService blocoService;

    public Apartamento cadastrar(ApartamentoRequestDTO request) {
        Bloco bloco = blocoService.buscarPorId(request.getBlocoId());

        validarCamposApartamento(request);

        apartamentoRepository.findByNumeroAndBloco_Id(request.getNumero(), request.getBlocoId())
                .ifPresent(a -> {
                    throw new OperacaoInvalidaException(
                            "Já existe um apartamento " + request.getNumero() +
                            " no bloco " + bloco.getNome());
                });

        if (request.getAndar() != null && bloco.getAndares() != null && request.getAndar() > bloco.getAndares()) {
            throw new OperacaoInvalidaException(
                    "O bloco " + bloco.getNome() + " possui apenas " + bloco.getAndares() + " andares.");
        }

        if (request.getAndar() != null && bloco.getApartamentosPorAndar() != null) {
            long existentes = apartamentoRepository.countByBloco_IdAndAndar(request.getBlocoId(), request.getAndar());
            if (existentes >= bloco.getApartamentosPorAndar()) {
                throw new OperacaoInvalidaException(
                        "O andar " + request.getAndar() + " já possui os " +
                        bloco.getApartamentosPorAndar() + " apartamentos definidos para o bloco " +
                        bloco.getNome() + ".");
            }
        }

        Apartamento apartamento = new Apartamento();
        apartamento.setNumero(request.getNumero());
        apartamento.setAndar(request.getAndar());
        apartamento.setBloco(bloco);
        apartamento.setQuartos(request.getQuartos());
        apartamento.setAreaMxComTotal(request.getAreaMxComTotal());
        apartamento.setObservacoes(request.getObservacoes());
        apartamento.setCondominioId(bloco.getCondominioId());

        return apartamentoRepository.save(apartamento);
    }

    public List<Apartamento> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return apartamentoRepository.findByCondominioId(condominioId);
        }
        return apartamentoRepository.findAll();
    }

    public List<Apartamento> listarAtivos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return apartamentoRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return apartamentoRepository.findByAtivo(true);
    }

    public List<Apartamento> listarPorBloco(UUID blocoId) {
        blocoService.buscarPorId(blocoId);
        return apartamentoRepository.findByBloco_Id(blocoId);
    }

    public List<Apartamento> listarPorBlocoAtivos(UUID blocoId) {
        blocoService.buscarPorId(blocoId);
        return apartamentoRepository.findByBloco_IdAndAtivo(blocoId, true);
    }

    public Apartamento buscarPorId(UUID id) {
        return apartamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado com id: " + id));
    }

    public Apartamento atualizar(UUID id, ApartamentoRequestDTO request) {
        Apartamento apartamento = buscarPorId(id);
        Bloco bloco = blocoService.buscarPorId(request.getBlocoId());

        validarCamposApartamento(request);

        if (request.getAndar() != null && bloco.getAndares() != null && request.getAndar() > bloco.getAndares()) {
            throw new OperacaoInvalidaException(
                    "O bloco " + bloco.getNome() + " possui apenas " + bloco.getAndares() + " andares.");
        }

        boolean andarMudou = request.getAndar() != null && !request.getAndar().equals(apartamento.getAndar());
        if (andarMudou && bloco.getApartamentosPorAndar() != null) {
            long existentes = apartamentoRepository.countByBloco_IdAndAndar(request.getBlocoId(), request.getAndar());
            if (existentes >= bloco.getApartamentosPorAndar()) {
                throw new OperacaoInvalidaException(
                        "O andar " + request.getAndar() + " já possui os " +
                        bloco.getApartamentosPorAndar() + " apartamentos definidos para o bloco " +
                        bloco.getNome() + ".");
            }
        }

        apartamento.setNumero(request.getNumero());
        apartamento.setAndar(request.getAndar());
        apartamento.setBloco(bloco);
        apartamento.setQuartos(request.getQuartos());
        apartamento.setAreaMxComTotal(request.getAreaMxComTotal());
        apartamento.setObservacoes(request.getObservacoes());
        apartamento.setAtualizadoEm(LocalDateTime.now());

        return apartamentoRepository.save(apartamento);
    }

    public void desativar(UUID id) {
        Apartamento apartamento = buscarPorId(id);
        apartamento.setAtivo(false);
        apartamento.setAtualizadoEm(LocalDateTime.now());
        apartamentoRepository.save(apartamento);
    }

    public void ativar(UUID id) {
        Apartamento apartamento = buscarPorId(id);
        apartamento.setAtivo(true);
        apartamento.setAtualizadoEm(LocalDateTime.now());
        apartamentoRepository.save(apartamento);
    }

    public void deletar(UUID id) {
        if (!apartamentoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Apartamento não encontrado com id: " + id);
        }
        apartamentoRepository.deleteById(id);
    }

    private void validarCamposApartamento(ApartamentoRequestDTO request) {
        if (request.getAndar() != null && request.getAndar() < 1) {
            throw new OperacaoInvalidaException("Informe um número inteiro maior que zero.");
        }
        if (request.getQuartos() == null) {
            throw new OperacaoInvalidaException("Quantidade de quartos é obrigatória.");
        }
        if (request.getQuartos() < MIN_QUARTOS || request.getQuartos() > MAX_QUARTOS) {
            throw new OperacaoInvalidaException(
                    "A quantidade de quartos deve estar entre " + MIN_QUARTOS + " e " + MAX_QUARTOS + ".");
        }
        if (request.getAreaMxComTotal() == null) {
            throw new OperacaoInvalidaException("Área total é obrigatória.");
        }
        if (request.getAreaMxComTotal() < MIN_AREA_M2 || request.getAreaMxComTotal() > MAX_AREA_M2) {
            throw new OperacaoInvalidaException(
                    "A área total deve estar entre " + (int) MIN_AREA_M2 + " e " + (int) MAX_AREA_M2 + " m².");
        }
    }
}
