package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Vaga;
import portaria.model.Apartamento;
import portaria.repository.VagaRepository;
import portaria.repository.ApartamentoRepository;
import portaria.security.AuthContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VagaService {

    private final VagaRepository vagaRepository;
    private final ApartamentoRepository apartamentoRepository;

    public Vaga cadastrar(Vaga vaga, UUID apartamentoId) {
        validarVagaUnica(vaga.getNumero());
        if (apartamentoId != null) {
            Apartamento apt = apartamentoRepository.findById(apartamentoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado: " + apartamentoId));
            vaga.setApartamento(apt);
        }
        var claims = AuthContext.get();
        if (claims != null && claims.condominioId() != null && !"default".equals(claims.condominioId())) {
            vaga.setCondominioId(claims.condominioId());
        }
        vaga.setAtiva(true);
        return vagaRepository.save(vaga);
    }

    public List<Vaga> listarTodas(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return vagaRepository.findByCondominioId(condominioId);
        }
        return vagaRepository.findAll();
    }

    public List<Vaga> listarApenasAtivas(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return vagaRepository.findByCondominioIdAndAtiva(condominioId, true);
        }
        return vagaRepository.findByAtiva(true);
    }

    public Vaga buscarPorId(String id) {
        return vagaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada com id: " + id));
    }

    public List<Vaga> buscarPorApartamento(UUID apartamentoId) {
        return vagaRepository.findByApartamentoId(apartamentoId);
    }

    public Vaga atualizar(String id, Vaga dados, String apartamentoIdStr) {
        Vaga vaga = buscarPorId(id);

        if (!vaga.getNumero().equals(dados.getNumero())) {
            validarVagaUnica(dados.getNumero());
        }

        vaga.setNumero(dados.getNumero());
        vaga.setLocalizacao(dados.getLocalizacao());
        vaga.setTipo(dados.getTipo());

        if (apartamentoIdStr != null) {
            if ("none".equals(apartamentoIdStr)) {
                vaga.setApartamento(null);
            } else {
                UUID aptId = UUID.fromString(apartamentoIdStr);
                Apartamento apt = apartamentoRepository.findById(aptId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado: " + aptId));
                vaga.setApartamento(apt);
            }
        }

        vaga.setAtualizadoEm(LocalDateTime.now());
        return vagaRepository.save(vaga);
    }

    public void desativar(String id) {
        Vaga vaga = buscarPorId(id);
        vaga.setAtiva(false);
        vaga.setAtualizadoEm(LocalDateTime.now());
        vagaRepository.save(vaga);
    }

    public void ativar(String id) {
        Vaga vaga = buscarPorId(id);
        vaga.setAtiva(true);
        vaga.setAtualizadoEm(LocalDateTime.now());
        vagaRepository.save(vaga);
    }

    private void validarVagaUnica(String numero) {
        vagaRepository.findByNumero(numero).ifPresent(v -> {
            throw new OperacaoInvalidaException("Já existe uma vaga cadastrada com o número: " + numero);
        });
    }
}
