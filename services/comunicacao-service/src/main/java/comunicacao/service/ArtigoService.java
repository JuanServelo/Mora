package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.repository.ArtigoConhecimentoRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.CondominioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtigoService {

    private final ArtigoConhecimentoRepository artigoRepository;

    public ArtigoConhecimento criar(ArtigoConhecimento artigo) {
        var claims = AuthContext.get();
        artigo.setCondominioId(CondominioUtils.condominioIdEfetivo());
        if (artigo.getAutor() == null && claims != null) {
            artigo.setAutor(claims.email());
        }
        return artigoRepository.save(artigo);
    }

    public ArtigoConhecimento publicar(UUID id) {
        ArtigoConhecimento artigo = buscarPorId(id);
        artigo.setPublicado(true);
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public ArtigoConhecimento atualizar(UUID id, ArtigoConhecimento dados) {
        ArtigoConhecimento artigo = buscarPorId(id);
        artigo.setTitulo(dados.getTitulo());
        artigo.setConteudo(dados.getConteudo());
        artigo.setCategoria(dados.getCategoria());
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public void excluir(UUID id) {
        if (!artigoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Artigo não encontrado com id: " + id);
        }
        artigoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ArtigoConhecimento buscarPorId(UUID id) {
        return artigoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> listarPublicados(String condominioId, CategoriaArtigo categoria) {
        if (categoria != null) return artigoRepository.findByCategoriaAndPublicadoTrue(categoria);
        if (condominioId != null) return artigoRepository.findByCondominioIdAndPublicadoTrue(condominioId);
        return artigoRepository.findByPublicadoTrue();
    }

    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> listarTodos(String condominioId) {
        if (condominioId != null) return artigoRepository.findByCondominioId(condominioId);
        return artigoRepository.findAll();
    }
}
