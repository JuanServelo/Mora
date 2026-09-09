package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.VeiculoResponseDTO;
import portaria.dto.atendimento.AtendimentoRegistroDTO;
import portaria.dto.atendimento.AtendimentoResponseDTO;
import portaria.dto.atendimento.AtendimentoVeiculoDTO;
import portaria.dto.atendimento.VagaStatusDTO;
import portaria.dto.atendimento.VisitanteResumoDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Apartamento;
import portaria.model.MovimentacaoVeiculo;
import portaria.model.Vaga;
import portaria.model.Veiculo;
import portaria.model.Visitante;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
import portaria.model.enums.TipoVisita;
import portaria.repository.ApartamentoRepository;
import portaria.repository.MovimentacaoVeiculoRepository;
import portaria.repository.VagaRepository;
import portaria.repository.VeiculoRepository;
import portaria.repository.VisitanteRepository;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AtendimentoService {

    private final VisitanteRepository visitanteRepository;
    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final MovimentacaoVeiculoRepository movimentacaoRepository;

    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$");
    private static final DateTimeFormatter DT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Busca por ID ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VisitanteResumoDTO buscarPorId(String id) {
        String condominioId = condominioIdObrigatorio();
        Visitante v = visitanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Visitante não encontrado."));
        validarMesmoCondo(v.getCondominioId(), condominioId);
        return VisitanteResumoDTO.fromEntity(v);
    }

    // ─── Busca ────────────────────────────────────────────────────────────────

    public List<VisitanteResumoDTO> buscar(String q, TipoVisita tipo) {
        String condominioId = condominioIdObrigatorio();
        String termo = q == null ? "" : q.trim();
        // Lista completa quando sem termo; busca limitada com termo
        PageRequest pageable = termo.isBlank() ? PageRequest.of(0, 200) : PageRequest.of(0, 20);
        return visitanteRepository
                .buscarPorNomeOuDocumento(condominioId, tipo, termo, pageable)
                .stream()
                .map(VisitanteResumoDTO::fromEntity)
                .toList();
    }

    // ─── Visitantes dentro agora ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VisitanteResumoDTO> dentro() {
        String condominioId = condominioIdObrigatorio();
        return visitanteRepository.findByCondominioIdAndStatus(condominioId, StatusAcesso.DENTRO)
                .stream()
                .map(VisitanteResumoDTO::fromEntity)
                .toList();
    }

    // ─── Histórico de visitantes com filtros opcionais ────────────────────────

    @Transactional(readOnly = true)
    public List<VisitanteResumoDTO> historico(String nome, String tipoVisita, String status) {
        String condominioId = condominioIdObrigatorio();
        return visitanteRepository.findByCondominioId(condominioId)
                .stream()
                .filter(v -> v.getHorarioEntrada() != null)
                .filter(v -> nome == null || nome.isBlank() ||
                        v.getNome().toLowerCase().contains(nome.toLowerCase()))
                .filter(v -> tipoVisita == null || tipoVisita.isBlank() ||
                        tipoVisita.equalsIgnoreCase(v.getTipoVisita().name()))
                .filter(v -> status == null || status.isBlank() ||
                        status.equalsIgnoreCase(v.getStatus().name()))
                .sorted((a, b) -> {
                    if (a.getHorarioEntrada() == null) return 1;
                    if (b.getHorarioEntrada() == null) return -1;
                    return b.getHorarioEntrada().compareTo(a.getHorarioEntrada());
                })
                .limit(200)
                .map(VisitanteResumoDTO::fromEntity)
                .toList();
    }

    // ─── Vagas da unidade com status de ocupação ──────────────────────────────

    @Transactional(readOnly = true)
    public List<VagaStatusDTO> vagasUnidade(UUID apartamentoId) {
        String condominioId = condominioIdObrigatorio();
        List<Vaga> vagas = vagaRepository.findByApartamentoId(apartamentoId)
                .stream()
                .filter(Vaga::isAtiva)
                .toList();

        return vagas.stream().map(vaga -> {
            var movAberta = movimentacaoRepository
                    .findFirstByCondominioIdAndVagaIdAndSaidaEmIsNull(condominioId, vaga.getId());

            if (movAberta.isPresent()) {
                MovimentacaoVeiculo m = movAberta.get();
                return VagaStatusDTO.builder()
                        .id(vaga.getId())
                        .numero(vaga.getNumero())
                        .tipo(vaga.getTipo())
                        .localizacao(vaga.getLocalizacao())
                        .disponivel(false)
                        .ocupadaPorPlaca(m.getPlaca())
                        .ocupadaDesde(m.getEntradaEm().format(DT_BR))
                        .build();
            }
            return VagaStatusDTO.builder()
                    .id(vaga.getId())
                    .numero(vaga.getNumero())
                    .tipo(vaga.getTipo())
                    .localizacao(vaga.getLocalizacao())
                    .disponivel(true)
                    .build();
        }).toList();
    }

    // ─── Registro atômico: pessoa + veículo + entrada ─────────────────────────

    public AtendimentoResponseDTO registrar(AtendimentoRegistroDTO dto) {
        JwtClaims claims = authRequired();
        String condominioId = condominioIdObrigatorio();
        TipoVisita tipoVisita = parseTipoVisita(dto.getTipoVisita());
        LocalDateTime agora = LocalDateTime.now();

        // 1. Resolver pessoa
        Visitante visitante = resolverPessoa(dto, tipoVisita, condominioId, agora);

        // RN-09: bloquear se já está dentro — AJUSTE 6: permitir se há veículo (entrada de veículo apenas)
        boolean pessoaJaDentro = visitante.getStatus() == StatusAcesso.DENTRO;
        if (pessoaJaDentro && dto.getVeiculo() == null) {
            throw new OperacaoInvalidaException(
                    visitante.getNome() + " já está no condomínio desde " +
                    visitante.getHorarioEntrada().format(DT_BR) + ".");
        }

        if (!pessoaJaDentro) {
            visitante.setHorarioEntrada(agora);
            visitante.setStatus(StatusAcesso.DENTRO);
            visitante.setAtualizadoEm(agora);
            visitante = visitanteRepository.save(visitante);
        }

        // 2. Resolver veículo (opcional)
        Veiculo veiculo = null;
        Vaga vagaUsada = null;

        if (dto.getVeiculo() != null) {
            AtendimentoVeiculoDTO vDto = dto.getVeiculo();
            String placaNorm = normalizarPlaca(vDto.getPlaca());
            validarPlaca(placaNorm);

            // RN-09: veículo já dentro
            movimentacaoRepository.findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(condominioId, placaNorm)
                    .ifPresent(m -> {
                        throw new OperacaoInvalidaException(
                                "O veículo " + placaNorm + " já está no condomínio desde " +
                                m.getEntradaEm().format(DT_BR) + ".");
                    });

            // AJUSTE 6: VISITA sem vagaId → rejeitar se não há vagas disponíveis
            if (tipoVisita == TipoVisita.VISITA && vDto.getVagaId() == null &&
                    dto.getApartamentoId() != null && !dto.getApartamentoId().isBlank()) {
                List<Vaga> vagasApt = vagaRepository
                        .findByApartamentoId(UUID.fromString(dto.getApartamentoId()))
                        .stream().filter(Vaga::isAtiva).toList();
                if (vagasApt.isEmpty()) {
                    throw new OperacaoInvalidaException(
                            "A unidade não possui vagas cadastradas. Registre o visitante sem veículo.");
                }
                boolean algumDisponivel = vagasApt.stream().anyMatch(v ->
                        !movimentacaoRepository.existsByCondominioIdAndVagaIdAndSaidaEmIsNull(condominioId, v.getId()));
                if (!algumDisponivel) {
                    throw new OperacaoInvalidaException(
                            "Não há vaga disponível na unidade. O veículo não pode ser cadastrado sem uma vaga.");
                }
            }

            veiculo = resolverVeiculo(vDto, placaNorm, visitante, tipoVisita, condominioId, agora);

            // Vaga: apenas para VISITA
            if (tipoVisita == TipoVisita.VISITA && vDto.getVagaId() != null) {
                Vaga vaga = vagaRepository.findById(vDto.getVagaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada."));
                // RN-09: vaga pode ter sido ocupada entre a seleção e a confirmação
                if (movimentacaoRepository.existsByCondominioIdAndVagaIdAndSaidaEmIsNull(condominioId, vaga.getId())) {
                    String placa = movimentacaoRepository
                            .findFirstByCondominioIdAndVagaIdAndSaidaEmIsNull(condominioId, vaga.getId())
                            .map(m -> " pelo veículo " + m.getPlaca()).orElse("");
                    throw new OperacaoInvalidaException(
                            "A vaga " + vaga.getNumero() + " foi ocupada" + placa + " antes da confirmação.");
                }
                veiculo.setVaga(vaga);
                vagaUsada = vaga;
            }

            veiculo.setStatus(StatusAcesso.DENTRO);
            veiculo.setDataEntrada(agora);
            veiculo.setAtualizadoEm(agora);
            veiculo = veiculoRepository.save(veiculo);

            // Registrar movimentação do veículo
            MovimentacaoVeiculo mov = new MovimentacaoVeiculo();
            mov.setPlaca(placaNorm);
            mov.setModelo(veiculo.getModelo());
            mov.setVeiculoId(veiculo.getId());
            mov.setVagaId(vagaUsada != null ? vagaUsada.getId() : null);
            mov.setCondominioId(condominioId);
            mov.setVinculadoNome(visitante.getNome());
            mov.setRegistradoPorEntradaId(claims.authUserId());
            mov.setEntradaEm(agora);
            movimentacaoRepository.save(mov);
        }

        // 3. Montar resposta
        String msg;
        if (pessoaJaDentro && veiculo != null) {
            msg = "Veículo " + veiculo.getPlaca() + " de " + visitante.getNome() + " registrado";
            msg += vagaUsada != null ? " na vaga " + vagaUsada.getNumero() + "." : ".";
        } else {
            msg = "Entrada de " + visitante.getNome() + " registrada.";
            if (veiculo != null && vagaUsada != null) {
                msg += " Veículo " + veiculo.getPlaca() + " na vaga " + vagaUsada.getNumero() + ".";
            } else if (veiculo != null) {
                msg += " Veículo " + veiculo.getPlaca() + " sem vaga.";
            }
        }

        return AtendimentoResponseDTO.builder()
                .pessoa(VisitanteResumoDTO.fromEntity(visitante))
                .veiculo(veiculo != null ? VeiculoResponseDTO.fromEntity(veiculo) : null)
                .vagaNumero(vagaUsada != null ? vagaUsada.getNumero() : null)
                .mensagem(msg)
                .build();
    }

    // ─── Saída ────────────────────────────────────────────────────────────────

    public VisitanteResumoDTO registrarSaida(String visitanteId) {
        String condominioId = condominioIdObrigatorio();
        Visitante visitante = visitanteRepository.findById(visitanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Visitante não encontrado."));
        validarMesmoCondo(visitante.getCondominioId(), condominioId);
        if (visitante.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Visitante não está registrado como DENTRO.");
        }
        LocalDateTime agora = LocalDateTime.now();
        visitante.setHorarioSaida(agora);
        visitante.setStatus(StatusAcesso.SAIU);
        visitante.setAtualizadoEm(agora);
        Visitante salvo = visitanteRepository.save(visitante);

        // AJUSTE 5: informar se há veículo com entrada em aberto
        VisitanteResumoDTO.VisitanteResumoDTOBuilder b = VisitanteResumoDTO.builderFromEntity(salvo);
        Veiculo veiculoDentro = veiculoRepository.findFirstByProprietarioIdAndStatus(salvo.getId(), StatusAcesso.DENTRO).orElse(null);
        if (veiculoDentro != null) {
            b.veiculoDentroPlaca(veiculoDentro.getPlaca());
            if (veiculoDentro.getVaga() != null) b.vagaDentroNumero(veiculoDentro.getVaga().getNumero());
        }
        return b.build();
    }

    // ─── Helpers internos ─────────────────────────────────────────────────────

    private Visitante resolverPessoa(AtendimentoRegistroDTO dto, TipoVisita tipoVisita,
                                     String condominioId, LocalDateTime agora) {
        if (dto.getPessoaId() != null && !dto.getPessoaId().isBlank()) {
            Visitante v = visitanteRepository.findById(dto.getPessoaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Visitante não encontrado: " + dto.getPessoaId()));
            validarMesmoCondo(v.getCondominioId(), condominioId);
            // Propaga o tipo selecionado — evita gravar Visitante quando o tipo escolhido é Terceiro
            v.setTipoVisita(tipoVisita);
            if (tipoVisita == TipoVisita.SERVICO) {
                v.setApartamento(null);
                v.setBloco(null);
            }
            if (dto.getNome() != null && !dto.getNome().isBlank()) v.setNome(dto.getNome());
            if (dto.getTelefone() != null) v.setTelefone(dto.getTelefone());
            if (dto.getObs() != null) v.setMotivoVisita(dto.getObs());
            if (dto.getEmpresa() != null) v.setEmpresa(dto.getEmpresa());
            if (dto.getDestino() != null) v.setDestino(dto.getDestino());
            return v;
        }

        // Novo cadastro
        String docNorm = normalizarDocumento(dto.getDocumento());

        if (!cpfValido(docNorm)) {
            throw new OperacaoInvalidaException("CPF inválido. Verifique os dígitos informados.");
        }

        // Reutilizar cadastro existente pelo CPF (evita duplicatas, mantém histórico)
        Visitante existentePorCpf = visitanteRepository.findByCondominioIdAndCpf(condominioId, docNorm).orElse(null);
        if (existentePorCpf != null) {
            // Propaga o tipo selecionado — evita gravar Visitante quando o tipo escolhido é Terceiro
            existentePorCpf.setTipoVisita(tipoVisita);
            if (tipoVisita == TipoVisita.SERVICO) {
                existentePorCpf.setApartamento(null);
                existentePorCpf.setBloco(null);
            }
            if (dto.getNome() != null && !dto.getNome().isBlank()) existentePorCpf.setNome(dto.getNome());
            if (dto.getTelefone() != null) existentePorCpf.setTelefone(dto.getTelefone());
            if (dto.getObs() != null) existentePorCpf.setMotivoVisita(dto.getObs());
            if (dto.getEmpresa() != null) existentePorCpf.setEmpresa(dto.getEmpresa());
            if (dto.getDestino() != null) existentePorCpf.setDestino(dto.getDestino());
            return existentePorCpf;
        }

        Visitante v = new Visitante();
        v.setNome(dto.getNome());
        v.setCpf(docNorm);          // cpf é a chave de dedup no Usuario base
        v.setDocumento(dto.getDocumento()); // preserva formato original
        v.setTelefone(dto.getTelefone());
        v.setMotivoVisita(dto.getObs());
        v.setTipoVisita(tipoVisita);
        v.setEmpresa(dto.getEmpresa());
        v.setDestino(dto.getDestino());
        v.setCondominioId(condominioId);
        v.setAtivo(true);
        v.setCriadoEm(agora);
        v.setAtualizadoEm(agora);

        if (tipoVisita == TipoVisita.VISITA) {
            if (dto.getApartamentoId() == null || dto.getApartamentoId().isBlank()) {
                throw new OperacaoInvalidaException("Unidade visitada é obrigatória para Visitante.");
            }
            Apartamento apt = apartamentoRepository.findById(UUID.fromString(dto.getApartamentoId()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Apartamento não encontrado."));
            validarMesmoCondo(apt.getCondominioId(), condominioId);
            v.setApartamento(apt);
            v.setBloco(apt.getBloco());
        }

        return v;
    }

    private Veiculo resolverVeiculo(AtendimentoVeiculoDTO vDto, String placa,
                                    Visitante visitante, TipoVisita tipoVisita,
                                    String condominioId, LocalDateTime agora) {
        Veiculo v;

        if (vDto.getVeiculoId() != null && !vDto.getVeiculoId().isBlank()) {
            v = veiculoRepository.findById(vDto.getVeiculoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado."));
            // Atualiza dados se enviados
            if (vDto.getModelo() != null) v.setModelo(vDto.getModelo());
            if (vDto.getCor() != null) v.setCor(vDto.getCor());
        } else {
            // Busca por placa no mesmo condomínio
            v = veiculoRepository.findByPlacaAndCondominioId(placa, condominioId)
                    .orElseGet(() -> {
                        Veiculo novo = new Veiculo();
                        novo.setPlaca(placa);
                        novo.setCondominioId(condominioId);
                        novo.setStatus(StatusAcesso.SAIU);
                        novo.setCriadoEm(agora);
                        return novo;
                    });
            if (vDto.getModelo() != null) v.setModelo(vDto.getModelo());
            if (vDto.getCor() != null) v.setCor(vDto.getCor());
        }

        // Tipo e proprietário derivados do tipo de visita
        v.setProprietarioId(visitante.getId());
        v.setProprietarioNome(visitante.getNome());
        v.setProprietarioCpf(visitante.getDocumento());
        if (tipoVisita == TipoVisita.VISITA) {
            v.setTipoProprietario(TipoProprietario.VISITANTE);
            v.setCategoria(CategoriaVeiculo.CARRO);
        } else {
            v.setTipoProprietario(TipoProprietario.FUNCIONARIO);
            v.setCategoria(CategoriaVeiculo.VEICULO_SERVICO);
            v.setVaga(null); // terceiro nunca tem vaga
        }

        return v;
    }

    private TipoVisita parseTipoVisita(String s) {
        try {
            return TipoVisita.valueOf(s.toUpperCase());
        } catch (Exception e) {
            throw new OperacaoInvalidaException("Tipo de visita inválido: " + s);
        }
    }

    private String normalizarDocumento(String doc) {
        return doc == null ? "" : doc.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }

    private String normalizarPlaca(String placa) {
        return placa == null ? "" : placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private void validarPlaca(String placa) {
        if (!PLACA.matcher(placa).matches()) {
            throw new OperacaoInvalidaException(
                    "Placa inválida: " + placa + ". Use AAA9999 ou AAA9A99 (Mercosul).");
        }
    }

    private boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) return false;
        if (cpf.chars().distinct().count() == 1) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (cpf.charAt(i) - '0') * (10 - i);
        int rem = 11 - (sum % 11);
        int dig1 = rem >= 10 ? 0 : rem;
        if (dig1 != cpf.charAt(9) - '0') return false;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += (cpf.charAt(i) - '0') * (11 - i);
        rem = 11 - (sum % 11);
        int dig2 = rem >= 10 ? 0 : rem;
        return dig2 == cpf.charAt(10) - '0';
    }

    private void validarMesmoCondo(String condoEntidade, String condoAtual) {
        if (condoAtual != null && !condoAtual.equals(condoEntidade)) {
            throw new AcessoNegadoException("Entidade pertence a outro condomínio.");
        }
    }

    private String condominioIdObrigatorio() {
        String cid = CondominioUtils.condominioIdEfetivo();
        if (cid == null) throw new AcessoNegadoException("Condomínio não identificado na sessão.");
        return cid;
    }

    private JwtClaims authRequired() {
        JwtClaims claims = AuthContext.get();
        if (claims == null) throw new AcessoNegadoException("Autenticação necessária.");
        return claims;
    }
}
