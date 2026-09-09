package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.PreAutorizacaoRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.PreAutorizacao;
import portaria.repository.PreAutorizacaoRepository;
import portaria.security.CondominioUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PreAutorizacaoService {

    private final PreAutorizacaoRepository repository;

    public PreAutorizacao cadastrar(UUID moradorId, PreAutorizacaoRequestDTO request) {
        if (request.validadeFim().isBefore(request.validadeInicio())) {
            throw new OperacaoInvalidaException("Data fim não pode ser anterior à data de início");
        }
        if (request.validadeFim().isBefore(LocalDate.now())) {
            throw new OperacaoInvalidaException("Data fim não pode ser no passado");
        }

        String condominioId = CondominioUtils.condominioIdEfetivo();

        PreAutorizacao pa = new PreAutorizacao();
        pa.setMoradorId(moradorId);
        pa.setCondominioId(condominioId);
        pa.setNomeVisitante(request.nomeVisitante());
        pa.setCpfVisitante(request.cpfVisitante());
        pa.setValidadeInicio(request.validadeInicio());
        pa.setValidadeFim(request.validadeFim());
        pa.setObservacoes(request.observacoes());

        return repository.save(pa);
    }

    public void revogar(String id) {
        PreAutorizacao pa = buscarPorId(id);
        pa.setAtivo(false);
        pa.setAtualizadoEm(LocalDateTime.now());
        repository.save(pa);
    }

    @Transactional(readOnly = true)
    public PreAutorizacao buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pré-autorização não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PreAutorizacao> listarDoMorador(UUID moradorId) {
        return repository.findByMoradorId(moradorId);
    }

    @Transactional(readOnly = true)
    public List<PreAutorizacao> listarAtivasHoje(String condominioId) {
        if (condominioId == null) return repository.findTodasAtivasHoje(LocalDate.now());
        return repository.findAtivasHoje(condominioId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<PreAutorizacao> buscarPorNomeOuCpf(String condominioId, String termo) {
        if (condominioId == null) {
            return repository.findTodasAtivasHoje(LocalDate.now()).stream()
                    .filter(p -> p.getNomeVisitante().toLowerCase().contains(termo.toLowerCase())
                            || termo.equals(p.getCpfVisitante()))
                    .toList();
        }
        return repository.buscarPorNomeOuCpf(condominioId, LocalDate.now(), termo);
    }
}
