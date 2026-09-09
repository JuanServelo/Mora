package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import portaria.dto.chave.*;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Chave;
import portaria.model.MovimentacaoChave;
import portaria.model.enums.TipoLocal;
import portaria.model.enums.TipoResponsavel;
import portaria.repository.*;
import portaria.security.AuthContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChaveService {

    private final ChaveRepository chaveRepository;
    private final MovimentacaoChaveRepository movimentacaoRepository;
    private final BlocoRepository blocoRepository;
    private final AreaComunRepository areaComunRepository;
    private final MoradorService moradorService;
    private final FuncionarioService funcionarioService;

    // ─── Locais elegíveis (RN-01) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LocalChaveDTO> listarLocaisElegiveis(String condominioId) {
        List<LocalChaveDTO> resultado = new ArrayList<>();
        if (condominioId != null && !condominioId.isBlank() && !"default".equals(condominioId)) {
            blocoRepository.findByCondominioIdAndAtivo(condominioId, true)
                .forEach(b -> resultado.add(new LocalChaveDTO(b.getId().toString(), b.getNome(), TipoLocal.BLOCO)));
            areaComunRepository.findByCondominioIdAndAtivo(condominioId, true)
                .forEach(a -> resultado.add(new LocalChaveDTO(a.getId(), a.getNome(), TipoLocal.AREA_COMUM)));
        } else {
            blocoRepository.findByAtivo(true)
                .forEach(b -> resultado.add(new LocalChaveDTO(b.getId().toString(), b.getNome(), TipoLocal.BLOCO)));
            areaComunRepository.findByAtivo(true)
                .forEach(a -> resultado.add(new LocalChaveDTO(a.getId(), a.getNome(), TipoLocal.AREA_COMUM)));
        }
        return resultado;
    }

    // ─── Cadastro (RN-01, RN-02, RN-03, RN-04) ────────────────────────────────

    public ChaveResponseDTO cadastrar(CadastrarChaveDTO dto) {
        var claims = AuthContext.get();
        String condominioId = (claims != null && !"default".equals(claims.condominioId()))
            ? claims.condominioId() : null;

        // RN-02: validar que o local pertence ao condomínio do porteiro
        String localNome = resolverLocalNome(dto.getLocalId(), dto.getTipoLocal(), condominioId);

        // RN-04: unicidade case-insensitive por local
        String nomeNormalizado = dto.getNomeChave().trim().toLowerCase();
        chaveRepository.findByLocalIdAndNomeNormalizado(dto.getLocalId(), nomeNormalizado)
            .ifPresent(c -> { throw new OperacaoInvalidaException(
                "Já existe uma chave com este nome neste local."); });

        Chave chave = new Chave();
        chave.setNomeChave(dto.getNomeChave().trim());
        chave.setNomeNormalizado(nomeNormalizado);
        chave.setDescricao(dto.getDescricao());
        chave.setLocalId(dto.getLocalId());
        chave.setTipoLocal(dto.getTipoLocal());
        chave.setLocalNome(localNome);
        chave.setCondominioId(condominioId);
        chave.setDisponivel(true);

        return ChaveResponseDTO.from(chaveRepository.save(chave), null);
    }

    // ─── Listagem ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChaveResponseDTO> listarTodas(String condominioId) {
        List<Chave> chaves = (condominioId != null && !condominioId.isBlank() && !"default".equals(condominioId))
            ? chaveRepository.findByCondominioId(condominioId)
            : chaveRepository.findAll();

        return chaves.stream().map(c -> {
            MovimentacaoChave aberto = movimentacaoRepository
                .findByChaveIdAndDataDevolucaoIsNull(c.getId()).orElse(null);
            return ChaveResponseDTO.from(c, aberto);
        }).toList();
    }

    @Transactional(readOnly = true)
    public ChaveResponseDTO buscarPorId(String id) {
        Chave chave = chaveRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chave não encontrada: " + id));
        MovimentacaoChave aberto = movimentacaoRepository
            .findByChaveIdAndDataDevolucaoIsNull(id).orElse(null);
        return ChaveResponseDTO.from(chave, aberto);
    }

    // ─── Retirada (RN-05) ──────────────────────────────────────────────────────

    public ChaveResponseDTO retirar(String id, RetirarChaveRequest request) {
        Chave chave = chaveRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chave não encontrada: " + id));

        // RN-05: bloquear se já há ciclo em aberto
        movimentacaoRepository.findByChaveIdAndDataDevolucaoIsNull(id).ifPresent(aberto -> {
            throw new OperacaoInvalidaException(
                "Esta chave já está em posse de " + aberto.getNomeResponsavel()
                + " desde " + fmtDataHora(aberto.getDataRetirada())
                + ". Registre a devolução antes de uma nova retirada.");
        });

        String nomeResponsavel = resolverNomeResponsavel(request.getResponsavelId(), request.getTipoResponsavel(), request.getNomeResponsavel());
        String perfilResponsavel = switch (request.getTipoResponsavel()) {
            case MORADOR -> "Morador";
            case FUNCIONARIO -> "Funcionário";
            case TERCEIRO -> "Terceiro";
        };

        var claims = AuthContext.get();
        String porteiroPorNome = claims != null ? claims.email() : "sistema";

        MovimentacaoChave mov = new MovimentacaoChave();
        mov.setChaveId(id);
        mov.setChaveNome(chave.getNomeChave());
        mov.setResponsavelId(request.getResponsavelId());
        mov.setNomeResponsavel(nomeResponsavel);
        mov.setPerfilResponsavel(perfilResponsavel);
        mov.setDataRetirada(LocalDateTime.now());
        mov.setRegistradoPorId(claims != null ? claims.authUserId() : null);
        mov.setRegistradoPorNome(porteiroPorNome);
        mov.setCondominioId(chave.getCondominioId());
        movimentacaoRepository.save(mov);

        chave.setDisponivel(false);
        chaveRepository.save(chave);

        return ChaveResponseDTO.from(chave, mov);
    }

    // ─── Devolução (RN-11) ─────────────────────────────────────────────────────

    public ChaveResponseDTO devolver(String id) {
        Chave chave = chaveRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chave não encontrada: " + id));

        MovimentacaoChave aberto = movimentacaoRepository.findByChaveIdAndDataDevolucaoIsNull(id)
            .orElseThrow(() -> new OperacaoInvalidaException("Chave já está disponível."));

        aberto.setDataDevolucao(LocalDateTime.now());
        movimentacaoRepository.save(aberto);

        chave.setDisponivel(true);
        chaveRepository.save(chave);

        return ChaveResponseDTO.from(chave, null);
    }

    // ─── Exclusão ──────────────────────────────────────────────────────────────

    public void deletar(String id) {
        Chave chave = chaveRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chave não encontrada: " + id));
        if (!chave.isDisponivel()) {
            throw new OperacaoInvalidaException("Não é possível excluir uma chave que está retirada.");
        }
        chaveRepository.delete(chave);
    }

    // ─── Histórico (RN-07 a RN-10) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MovimentacaoChaveResponseDTO> buscarHistorico(
        String chaveId,
        LocalDate dataInicio,
        LocalDate dataFim,
        String quemRetirou,
        String perfil,
        String status
    ) {
        chaveRepository.findById(chaveId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chave não encontrada: " + chaveId));

        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.of(2000, 1, 1);
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now();

        if (inicio.isAfter(fim)) {
            throw new OperacaoInvalidaException("A data inicial não pode ser posterior à data final.");
        }

        LocalDateTime filtroInicio = inicio.atStartOfDay();
        LocalDateTime filtroFim = fim.atTime(LocalTime.MAX);

        String quemPattern = (quemRetirou != null && !quemRetirou.isBlank())
            ? "%" + quemRetirou.trim().toLowerCase() + "%" : null;
        String perfilParam = (perfil != null && !perfil.isBlank()) ? perfil.trim() : null;
        String statusParam = (status != null && !status.isBlank()) ? status.trim() : null;

        return movimentacaoRepository
            .buscarHistoricoFiltrado(chaveId, filtroInicio, filtroFim, quemPattern, perfilParam, statusParam)
            .stream()
            .map(MovimentacaoChaveResponseDTO::from)
            .toList();
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private String resolverLocalNome(String localId, TipoLocal tipo, String condominioId) {
        if (tipo == TipoLocal.BLOCO) {
            var bloco = blocoRepository.findById(java.util.UUID.fromString(localId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado: " + localId));
            // RN-02: escopo
            if (condominioId != null && !condominioId.equals(bloco.getCondominioId())) {
                throw new AcessoNegadoException("Bloco não pertence ao seu condomínio.");
            }
            return bloco.getNome();
        } else {
            var area = areaComunRepository.findById(localId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Área comum não encontrada: " + localId));
            if (condominioId != null && !condominioId.equals(area.getCondominioId())) {
                throw new AcessoNegadoException("Área comum não pertence ao seu condomínio.");
            }
            return area.getNome();
        }
    }

    private String resolverNomeResponsavel(String responsavelId, TipoResponsavel tipo, String nomeResponsavel) {
        if (nomeResponsavel != null && !nomeResponsavel.isBlank()) {
            return nomeResponsavel;
        }
        return switch (tipo) {
            case MORADOR -> moradorService.buscarPorId(responsavelId).getNome();
            case FUNCIONARIO -> funcionarioService.buscarPorId(responsavelId).getNome();
            case TERCEIRO -> responsavelId; // Terceiro não tem registro no portaria-service
        };
    }

    private static String fmtDataHora(LocalDateTime dt) {
        if (dt == null) return "data desconhecida";
        return String.format("%02d/%02d/%04d %02d:%02d",
            dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(),
            dt.getHour(), dt.getMinute());
    }
}
