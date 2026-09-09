package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.aluguel.AluguelVagaRequestDTO;
import portaria.dto.aluguel.DisponibilidadeVagaRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.AluguelVaga;
import portaria.model.DisponibilidadeVaga;
import portaria.model.Vaga;
import portaria.model.enums.ModalidadeAluguel;
import portaria.model.enums.StatusAluguel;
import portaria.repository.AluguelVagaRepository;
import portaria.repository.DisponibilidadeVagaRepository;
import portaria.repository.VagaRepository;
import portaria.security.AuthContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AluguelService {

    private static final BigDecimal VALOR_DIARIA = new BigDecimal("15.00");
    private static final BigDecimal VALOR_MENSAL = new BigDecimal("200.00");
    private static final int DIAS_CANCELAMENTO_SEM_PENALIDADE = 3;
    private static final BigDecimal PERCENTUAL_PENALIDADE = new BigDecimal("0.20");

    private final VagaRepository vagaRepository;
    private final DisponibilidadeVagaRepository disponibilidadeRepository;
    private final AluguelVagaRepository aluguelRepository;

    // ── Disponibilidade ───────────────────────────────────────────────────────

    @Transactional
    public DisponibilidadeVaga disponibilizar(DisponibilidadeVagaRequestDTO dto, UUID proprietarioId) {
        validarPeriodo(dto.dataInicio(), dto.dataFim());

        Vaga vaga = buscarVaga(dto.vagaId());

        List<DisponibilidadeVaga> conflitos = disponibilidadeRepository
                .findConflitantes(dto.vagaId(), dto.dataInicio(), dto.dataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Conflito de datas: a vaga já está disponibilizada ou alugada neste período.");
        }

        DisponibilidadeVaga disp = new DisponibilidadeVaga();
        disp.setProprietarioId(proprietarioId);
        disp.setVaga(vaga);
        disp.setDataInicio(dto.dataInicio());
        disp.setDataFim(dto.dataFim());
        disp.setAtiva(true);
        setCondominioId(disp);
        return disponibilidadeRepository.save(disp);
    }

    public List<DisponibilidadeVaga> listarDisponibilidadesAtivas(String condominioId) {
        if (condominioId != null) {
            return disponibilidadeRepository.findByCondominioIdAndAtivaTrue(condominioId);
        }
        return disponibilidadeRepository.findByAtivaTrue();
    }

    public List<DisponibilidadeVaga> listarMinhasDisponibilidades(UUID proprietarioId) {
        return disponibilidadeRepository.findByProprietarioIdAndAtivaTrue(proprietarioId);
    }

    @Transactional
    public void cancelarDisponibilidade(String disponibilidadeId, UUID proprietarioId) {
        DisponibilidadeVaga disp = disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Disponibilidade não encontrada: " + disponibilidadeId));

        if (!disp.getProprietarioId().equals(proprietarioId)) {
            throw new OperacaoInvalidaException(
                    "Apenas o proprietário pode cancelar esta disponibilidade.");
        }
        disp.setAtiva(false);
        disponibilidadeRepository.save(disp);
    }

    // ── Solicitação de aluguel ────────────────────────────────────────────────

    @Transactional
    public AluguelVaga solicitar(AluguelVagaRequestDTO dto, UUID solicitanteId) {
        validarPeriodo(dto.dataInicio(), dto.dataFim());

        Vaga vaga = buscarVaga(dto.vagaId());
        if (!vaga.isAtiva()) {
            throw new OperacaoInvalidaException("Vaga inativa, não é possível alugar.");
        }

        List<DisponibilidadeVaga> disponibilidades = disponibilidadeRepository
                .findConflitantes(dto.vagaId(), dto.dataInicio(), dto.dataFim());
        if (disponibilidades.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "A vaga não está disponível para o período solicitado.");
        }

        List<AluguelVaga> conflitos = aluguelRepository
                .findAprovadosConflitantes(dto.vagaId(), dto.dataInicio(), dto.dataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "A vaga acabou de ficar indisponível: outro aluguel foi aprovado para este período.");
        }

        UUID proprietarioId = disponibilidades.get(0).getProprietarioId();
        if (proprietarioId.equals(solicitanteId)) {
            throw new OperacaoInvalidaException(
                    "O proprietário da vaga não pode alugar a própria vaga.");
        }

        BigDecimal valorTotal = calcularValor(dto.modalidade(), dto.dataInicio(), dto.dataFim());

        AluguelVaga aluguel = new AluguelVaga();
        aluguel.setSolicitanteId(solicitanteId);
        aluguel.setProprietarioId(proprietarioId);
        aluguel.setVaga(vaga);
        aluguel.setDataInicio(dto.dataInicio());
        aluguel.setDataFim(dto.dataFim());
        aluguel.setModalidade(dto.modalidade());
        aluguel.setValorTotal(valorTotal);
        aluguel.setStatus(StatusAluguel.PENDENTE);
        setCondominioId(aluguel);
        return aluguelRepository.save(aluguel);
    }

    // ── Aprovação / recusa ────────────────────────────────────────────────────

    @Transactional
    public AluguelVaga aprovar(String aluguelId, UUID responsavelId) {
        AluguelVaga aluguel = buscarAluguel(aluguelId);
        validarPendente(aluguel);
        validarResponsavelAprovacao(aluguel, responsavelId);

        List<AluguelVaga> conflitos = aluguelRepository
                .findAprovadosConflitantes(
                        aluguel.getVaga().getId(),
                        aluguel.getDataInicio(),
                        aluguel.getDataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Não é possível aprovar: já existe um aluguel aprovado para este período.");
        }

        aluguel.setStatus(StatusAluguel.APROVADO);
        aluguel.setAtualizadoEm(LocalDateTime.now());
        return aluguelRepository.save(aluguel);
    }

    @Transactional
    public AluguelVaga recusar(String aluguelId, UUID responsavelId, String justificativa) {
        AluguelVaga aluguel = buscarAluguel(aluguelId);
        validarPendente(aluguel);
        validarResponsavelAprovacao(aluguel, responsavelId);

        aluguel.setStatus(StatusAluguel.RECUSADO);
        aluguel.setMotivoRecusa(justificativa);
        aluguel.setAtualizadoEm(LocalDateTime.now());
        return aluguelRepository.save(aluguel);
    }

    // ── Cancelamento ──────────────────────────────────────────────────────────

    @Transactional
    public AluguelVaga cancelar(String aluguelId, UUID solicitanteId) {
        AluguelVaga aluguel = buscarAluguel(aluguelId);

        if (!aluguel.getSolicitanteId().equals(solicitanteId)) {
            throw new OperacaoInvalidaException(
                    "Apenas o solicitante pode cancelar este aluguel.");
        }
        if (aluguel.getStatus() == StatusAluguel.CANCELADO
                || aluguel.getStatus() == StatusAluguel.RECUSADO
                || aluguel.getStatus() == StatusAluguel.CONCLUIDO) {
            throw new OperacaoInvalidaException(
                    "Aluguel não pode ser cancelado no status atual: " + aluguel.getStatus());
        }

        long diasParaInicio = ChronoUnit.DAYS.between(LocalDate.now(), aluguel.getDataInicio());
        if (diasParaInicio < DIAS_CANCELAMENTO_SEM_PENALIDADE && aluguel.getValorTotal() != null) {
            aluguel.setValorPenalidade(aluguel.getValorTotal().multiply(PERCENTUAL_PENALIDADE));
        }

        aluguel.setStatus(StatusAluguel.CANCELADO);
        aluguel.setAtualizadoEm(LocalDateTime.now());
        return aluguelRepository.save(aluguel);
    }

    // ── Histórico ─────────────────────────────────────────────────────────────

    public List<AluguelVaga> historicoPorSolicitante(UUID solicitanteId) {
        return aluguelRepository.findBySolicitanteId(solicitanteId);
    }

    public List<AluguelVaga> historicoPorProprietario(UUID proprietarioId) {
        return aluguelRepository.findByProprietarioId(proprietarioId);
    }

    public List<AluguelVaga> historicoPorSolicitanteEStatus(UUID solicitanteId, StatusAluguel status) {
        return aluguelRepository.findBySolicitanteIdAndStatus(solicitanteId, status);
    }

    public List<AluguelVaga> historicoPorProprietarioEStatus(UUID proprietarioId, StatusAluguel status) {
        return aluguelRepository.findByProprietarioIdAndStatus(proprietarioId, status);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Vaga buscarVaga(String vagaId) {
        return vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada: " + vagaId));
    }

    private AluguelVaga buscarAluguel(String id) {
        return aluguelRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluguel não encontrado: " + id));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (!fim.isAfter(inicio)) {
            throw new OperacaoInvalidaException("A data de fim deve ser posterior à data de início.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new OperacaoInvalidaException("A data de início não pode ser no passado.");
        }
    }

    private void validarPendente(AluguelVaga aluguel) {
        if (aluguel.getStatus() != StatusAluguel.PENDENTE) {
            throw new OperacaoInvalidaException(
                    "Apenas aluguéis PENDENTES podem ser aprovados ou recusados. Status atual: "
                            + aluguel.getStatus());
        }
    }

    private void validarResponsavelAprovacao(AluguelVaga aluguel, UUID responsavelId) {
        if (!aluguel.getProprietarioId().equals(responsavelId)) {
            throw new OperacaoInvalidaException(
                    "Apenas o proprietário da vaga pode aprovar ou recusar esta solicitação.");
        }
    }

    private BigDecimal calcularValor(ModalidadeAluguel modalidade, LocalDate inicio, LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(inicio, fim);
        if (modalidade == ModalidadeAluguel.DIARIA) {
            return VALOR_DIARIA.multiply(BigDecimal.valueOf(dias));
        }
        long meses = ChronoUnit.MONTHS.between(inicio, fim);
        if (meses == 0) meses = 1;
        return VALOR_MENSAL.multiply(BigDecimal.valueOf(meses));
    }

    private void setCondominioId(DisponibilidadeVaga disp) {
        var claims = AuthContext.get();
        if (claims != null && claims.condominioId() != null && !"default".equals(claims.condominioId())) {
            disp.setCondominioId(claims.condominioId());
        }
    }

    private void setCondominioId(AluguelVaga aluguel) {
        var claims = AuthContext.get();
        if (claims != null && claims.condominioId() != null && !"default".equals(claims.condominioId())) {
            aluguel.setCondominioId(claims.condominioId());
        }
    }
}
