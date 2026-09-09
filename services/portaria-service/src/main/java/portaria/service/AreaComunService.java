package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.AreaComum;
import portaria.repository.AreaComunRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AreaComunService {

    private static final int MIN_CAPACIDADE = 1;
    private static final int MAX_CAPACIDADE = 500;
    private static final double MIN_AREA_COMUM_M2 = 5.0;
    private static final double MAX_AREA_COMUM_M2 = 10000.0;

    private final AreaComunRepository areaComunRepository;

    public AreaComum cadastrar(AreaComum areaComum) {
        validarCamposAreaComum(areaComum);

        if (areaComum.getNome() != null) {
            areaComum.setNome(areaComum.getNome().trim());
        }

        if (areaComum.getCondominioId() != null) {
            areaComunRepository.findByNomeIgnoreCaseAndCondominioId(areaComum.getNome(), areaComum.getCondominioId())
                    .ifPresent(a -> {
                        throw new OperacaoInvalidaException("Já existe uma área comum com este nome neste condomínio.");
                    });
        } else {
            areaComunRepository.findByNomeIgnoreCase(areaComum.getNome()).ifPresent(a -> {
                throw new OperacaoInvalidaException("Já existe uma área comum cadastrada com o nome: " + areaComum.getNome());
            });
        }
        return areaComunRepository.save(areaComum);
    }

    public List<AreaComum> listarTodas(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return areaComunRepository.findByCondominioId(condominioId);
        }
        return areaComunRepository.findAll();
    }

    public List<AreaComum> listarAtivas(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return areaComunRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return areaComunRepository.findByAtivo(true);
    }

    public List<AreaComum> listarPorTipo(String tipo) {
        return areaComunRepository.findByTipo(tipo);
    }

    public List<AreaComum> listarPorTipoAtivas(String tipo) {
        return areaComunRepository.findByTipo(tipo).stream().filter(AreaComum::isAtivo).toList();
    }

    public AreaComum buscarPorId(String id) {
        return areaComunRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Área comum não encontrada com id: " + id));
    }

    public AreaComum atualizar(String id, AreaComum dados) {
        AreaComum areaComum = buscarPorId(id);

        validarCamposAreaComum(dados);

        if (dados.getNome() != null) {
            dados.setNome(dados.getNome().trim());
        }

        String condId = areaComum.getCondominioId();

        if (!areaComum.getNome().equalsIgnoreCase(dados.getNome())) {
            if (condId != null) {
                areaComunRepository.findByNomeIgnoreCaseAndCondominioId(dados.getNome(), condId).ifPresent(a -> {
                    throw new OperacaoInvalidaException("Já existe uma área comum com este nome neste condomínio.");
                });
            } else {
                areaComunRepository.findByNomeIgnoreCase(dados.getNome()).ifPresent(a -> {
                    throw new OperacaoInvalidaException("Já existe uma área comum com o nome: " + dados.getNome());
                });
            }
            areaComum.setNome(dados.getNome());
        }

        areaComum.setTipo(dados.getTipo());
        areaComum.setDescricao(dados.getDescricao());
        areaComum.setLocalizacao(dados.getLocalizacao());
        areaComum.setCapacidadeMaxima(dados.getCapacidadeMaxima());
        areaComum.setArea(dados.getArea());
        areaComum.setPodeReservar(dados.isPodeReservar());
        areaComum.setObservacoes(dados.getObservacoes());
        areaComum.setAtualizadoEm(LocalDateTime.now());

        return areaComunRepository.save(areaComum);
    }

    public void desativar(String id) {
        AreaComum areaComum = buscarPorId(id);
        areaComum.setAtivo(false);
        areaComum.setAtualizadoEm(LocalDateTime.now());
        areaComunRepository.save(areaComum);
    }

    public void ativar(String id) {
        AreaComum areaComum = buscarPorId(id);
        areaComum.setAtivo(true);
        areaComum.setAtualizadoEm(LocalDateTime.now());
        areaComunRepository.save(areaComum);
    }

    public void deletar(String id) {
        if (!areaComunRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Área comum não encontrada com id: " + id);
        }
        areaComunRepository.deleteById(id);
    }

    private void validarCamposAreaComum(AreaComum areaComum) {
        if (areaComum.getCapacidadeMaxima() == null) {
            throw new OperacaoInvalidaException("Capacidade máxima é obrigatória.");
        }
        if (areaComum.getCapacidadeMaxima() < MIN_CAPACIDADE || areaComum.getCapacidadeMaxima() > MAX_CAPACIDADE) {
            throw new OperacaoInvalidaException(
                    "A capacidade máxima deve estar entre " + MIN_CAPACIDADE + " e " + MAX_CAPACIDADE + " pessoas.");
        }
        if (areaComum.getArea() == null) {
            throw new OperacaoInvalidaException("Área é obrigatória.");
        }
        if (areaComum.getArea() < MIN_AREA_COMUM_M2 || areaComum.getArea() > MAX_AREA_COMUM_M2) {
            throw new OperacaoInvalidaException(
                    "A área deve estar entre " + (int) MIN_AREA_COMUM_M2 + " e " + (int) MAX_AREA_COMUM_M2 + " m².");
        }
    }
}
