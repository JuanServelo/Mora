package comunicacao.service;

import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Aviso;
import comunicacao.repository.AvisoRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.CondominioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AvisoService {

    private final AvisoRepository avisoRepository;

    public Aviso criar(Aviso aviso) {
        var claims = AuthContext.get();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        aviso.setCondominioId(condominioId);
        if (aviso.getAutor() == null && claims != null) {
            aviso.setAutor(claims.email());
        }
        return avisoRepository.save(aviso);
    }

    public Aviso publicar(UUID id) {
        Aviso aviso = buscarPorId(id);
        aviso.setPublicado(true);
        aviso.setAtualizadoEm(LocalDateTime.now());
        return avisoRepository.save(aviso);
    }

    public Aviso encerrar(UUID id) {
        Aviso aviso = buscarPorId(id);
        aviso.setDataFim(LocalDate.now().minusDays(1));
        aviso.setAtualizadoEm(LocalDateTime.now());
        return avisoRepository.save(aviso);
    }

    public Aviso atualizar(UUID id, Aviso dados) {
        Aviso aviso = buscarPorId(id);
        aviso.setTitulo(dados.getTitulo());
        aviso.setMensagem(dados.getMensagem());
        aviso.setDataInicio(dados.getDataInicio());
        aviso.setDataFim(dados.getDataFim());
        aviso.setPublicoAlvo(dados.getPublicoAlvo());
        aviso.setAtualizadoEm(LocalDateTime.now());
        return avisoRepository.save(aviso);
    }

    public void excluir(UUID id) {
        if (!avisoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Aviso não encontrado com id: " + id);
        }
        avisoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Aviso buscarPorId(UUID id) {
        return avisoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Aviso> listarTodos(String condominioId) {
        if (condominioId == null) return avisoRepository.findAllByOrderByCriadoEmDesc();
        return avisoRepository.findByCondominioIdOrderByCriadoEmDesc(condominioId);
    }

    @Transactional(readOnly = true)
    public List<Aviso> listarAtivos(String condominioId) {
        if (condominioId == null) return avisoRepository.findTodosAtivos(LocalDate.now());
        return avisoRepository.findAtivos(condominioId, LocalDate.now());
    }
}
