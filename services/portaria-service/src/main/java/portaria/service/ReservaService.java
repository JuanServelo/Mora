package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.reserva.ReservaRequestDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.AreaComum;
import portaria.model.Reserva;
import portaria.model.enums.StatusReserva;
import portaria.repository.ReservaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AreaComunService areaComunService;

    public Reserva solicitar(UUID solicitanteId, ReservaRequestDTO request) {
        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new OperacaoInvalidaException("Data fim não pode ser anterior à data de início");
        }
        if (request.dataInicio().isBefore(LocalDate.now())) {
            throw new OperacaoInvalidaException("Não é possível reservar para datas passadas");
        }

        AreaComum area = areaComunService.buscarPorId(request.areaComunId());
        if (!area.isPodeReservar()) {
            throw new OperacaoInvalidaException("Esta área comum não está disponível para reserva");
        }
        if (!area.isAtivo()) {
            throw new OperacaoInvalidaException("Esta área comum está inativa");
        }

        List<Reserva> conflitos = reservaRepository.findConflitantes(
                area.getId(), request.dataInicio(), request.dataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Já existe uma reserva para este período em '" + area.getNome() + "'");
        }

        Reserva reserva = new Reserva();
        reserva.setSolicitanteId(solicitanteId);
        reserva.setAreaComum(area);
        reserva.setCondominioId(area.getCondominioId());
        reserva.setDataInicio(request.dataInicio());
        reserva.setDataFim(request.dataFim());
        reserva.setHoraInicio(request.horaInicio());
        reserva.setHoraFim(request.horaFim());
        reserva.setObservacoes(request.observacoes());
        reserva.setStatus(StatusReserva.PENDENTE);

        if (area.getTaxaLocacao() != null && area.getTaxaLocacao() > 0) {
            long dias = ChronoUnit.DAYS.between(request.dataInicio(), request.dataFim()) + 1;
            reserva.setValorTotal(BigDecimal.valueOf(area.getTaxaLocacao() * dias));
        }

        return reservaRepository.save(reserva);
    }

    public Reserva aprovar(String reservaId) {
        Reserva reserva = buscarPorId(reservaId);
        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new OperacaoInvalidaException("Apenas reservas PENDENTES podem ser aprovadas");
        }
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setAtualizadoEm(LocalDateTime.now());
        return reservaRepository.save(reserva);
    }

    public Reserva recusar(String reservaId, String motivo) {
        Reserva reserva = buscarPorId(reservaId);
        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new OperacaoInvalidaException("Apenas reservas PENDENTES podem ser recusadas");
        }
        reserva.setStatus(StatusReserva.RECUSADA);
        reserva.setMotivoRecusa(motivo);
        reserva.setAtualizadoEm(LocalDateTime.now());
        return reservaRepository.save(reserva);
    }

    public Reserva cancelar(String reservaId) {
        Reserva reserva = buscarPorId(reservaId);
        if (reserva.getStatus() == StatusReserva.CONCLUIDA || reserva.getStatus() == StatusReserva.CANCELADA) {
            throw new OperacaoInvalidaException("Não é possível cancelar uma reserva " + reserva.getStatus());
        }
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setAtualizadoEm(LocalDateTime.now());
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public Reserva buscarPorId(String id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorCondominio(String condominioId) {
        if (condominioId == null) return reservaRepository.findAll();
        return reservaRepository.findByCondominioId(condominioId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorCondominioEStatus(String condominioId, StatusReserva status) {
        if (condominioId == null) return reservaRepository.findAll()
                .stream().filter(r -> r.getStatus() == status).toList();
        return reservaRepository.findByCondominioIdAndStatus(condominioId, status);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarDoSolicitante(UUID solicitanteId) {
        return reservaRepository.findBySolicitanteId(solicitanteId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorArea(String areaComunId) {
        return reservaRepository.findByAreaComum_Id(areaComunId);
    }
}
