package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.AlterarVagaDTO;
import portaria.dto.CriarVeiculoDTO;
import portaria.dto.MovimentacaoVeiculoResponseDTO;
import portaria.dto.VeiculoResponseDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Funcionario;
import portaria.model.Morador;
import portaria.model.MovimentacaoVeiculo;
import portaria.model.Vaga;
import portaria.model.Veiculo;
import portaria.model.enums.CategoriaVeiculo;
import portaria.model.enums.StatusAcesso;
import portaria.model.enums.TipoProprietario;
import portaria.model.Visitante;
import portaria.repository.FuncionarioRepository;
import portaria.repository.MoradorRepository;
import portaria.repository.MovimentacaoVeiculoRepository;
import portaria.repository.VagaRepository;
import portaria.repository.VeiculoRepository;
import portaria.repository.VisitanteRepository;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final MoradorRepository moradorRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final VagaRepository vagaRepository;
    private final MovimentacaoVeiculoRepository movimentacaoRepository;
    private final VisitanteRepository visitanteRepository;

    private static final Pattern PLACA_PATTERN =
            Pattern.compile("^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$");
    private static final DateTimeFormatter DT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Grupos de perfis ──────────────────────────────────────────────────────

    private static final Set<String> ADMIN_PERFIS = Set.of(
            "ADMIN_GERAL", "ADMIN_SINDICO"
    );
    private static final Set<String> MORADOR_PERFIS = Set.of(
            "MORADOR", "DONO_ALUGUEL"
    );

    private boolean isAdmin(String perfil) { return ADMIN_PERFIS.contains(perfil); }
    private boolean isPorteiro(String perfil) { return "PORTEIRO".equals(perfil); }
    private boolean isMorador(String perfil) { return MORADOR_PERFIS.contains(perfil); }

    // ─── Helpers de autenticação ───────────────────────────────────────────────

    private JwtClaims authRequired() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.perfil() == null) {
            throw new AcessoNegadoException("Autenticação necessária.");
        }
        return claims;
    }

    private Optional<Morador> moradorDoClaims(JwtClaims claims) {
        if (claims.email() == null) return Optional.empty();
        return moradorRepository.findByEmail(claims.email());
    }

    // ─── Validação de permissão para edição (atualizar / alterar vaga) ─────────

    private void validarPermissaoEdicao(JwtClaims claims, Veiculo veiculo) {
        String perfil = claims.perfil();

        if (isAdmin(perfil)) return;

        if (isPorteiro(perfil)) return; // porteiro pode editar qualquer veículo do condo

        if (isMorador(perfil)) {
            if (veiculo.isVeiculoServico()) {
                throw new AcessoNegadoException("Moradores não podem alterar veículos de serviço.");
            }
            Morador atual = moradorDoClaims(claims)
                    .orElseThrow(() -> new AcessoNegadoException(
                            "Registro de morador não encontrado para o usuário autenticado."));
            if (!atual.getId().equals(veiculo.getProprietarioId())) {
                throw new AcessoNegadoException("Você só pode alterar seus próprios veículos.");
            }
            return;
        }

        throw new AcessoNegadoException("Perfil não autorizado para esta operação.");
    }

    // ─── Operações públicas ────────────────────────────────────────────────────

    public VeiculoResponseDTO cadastrar(CriarVeiculoDTO dto) {
        JwtClaims claims = authRequired();
        String perfil = claims.perfil();

        if (!isPorteiro(perfil) && !isAdmin(perfil) && !isMorador(perfil)) {
            throw new AcessoNegadoException("Perfil não autorizado para cadastrar veículos.");
        }

        // Moradores só podem cadastrar para si mesmos, via caminho legado (sem tipoProprietario)
        TipoProprietario tipo = resolverTipo(dto, perfil, claims);

        String placaNorm = dto.getPlaca().toUpperCase().trim();
        validarFormatoPlaca(placaNorm);
        validarPlacaUnica(placaNorm, null);

        String condominioId = CondominioUtils.condominioIdEfetivo();

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placaNorm);
        veiculo.setModelo(dto.getModelo());
        veiculo.setCor(dto.getCor());
        veiculo.setObs(dto.getObs());
        veiculo.setTipoProprietario(tipo);
        veiculo.setCategoria(categoriaParaTipo(tipo));
        veiculo.setStatus(StatusAcesso.SAIU);
        veiculo.setCriadoEm(LocalDateTime.now());
        veiculo.setAtualizadoEm(LocalDateTime.now());
        if (condominioId != null && !"default".equals(condominioId)) {
            veiculo.setCondominioId(condominioId);
        }

        switch (tipo) {
            case MORADOR -> configurarVeiculoMorador(dto, veiculo, condominioId);
            case VISITANTE -> configurarVeiculoVisitante(dto, veiculo, condominioId, perfil, claims);
            case FUNCIONARIO -> configurarVeiculoServico(dto, veiculo, condominioId);
        }

        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    private TipoProprietario resolverTipo(CriarVeiculoDTO dto, String perfil, JwtClaims claims) {
        if (dto.getTipoProprietario() != null) {
            return dto.getTipoProprietario();
        }
        // Caminho legado: derivar do campo categoria
        if (dto.getCategoria() != null) {
            return switch (dto.getCategoria()) {
                case VEICULO_SERVICO -> TipoProprietario.FUNCIONARIO;
                default -> TipoProprietario.MORADOR;
            };
        }
        // Morador sem tipo explícito → MORADOR por padrão
        if (isMorador(perfil)) return TipoProprietario.MORADOR;
        throw new OperacaoInvalidaException("Tipo de veículo é obrigatório.");
    }

    private CategoriaVeiculo categoriaParaTipo(TipoProprietario tipo) {
        return tipo == TipoProprietario.FUNCIONARIO ? CategoriaVeiculo.VEICULO_SERVICO : CategoriaVeiculo.CARRO;
    }

    private void configurarVeiculoMorador(CriarVeiculoDTO dto, Veiculo veiculo, String condominioId) {
        if (dto.getProprietarioId() == null || dto.getProprietarioId().isBlank()) {
            throw new OperacaoInvalidaException("Morador vinculado é obrigatório para veículos de Morador.");
        }
        if (dto.getVagaId() == null || dto.getVagaId().isBlank()) {
            throw new OperacaoInvalidaException("Vaga é obrigatória para veículos de Morador.");
        }

        Morador morador = moradorRepository.findByIdComApartamento(dto.getProprietarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado: " + dto.getProprietarioId()));
        validarMesmoCondo(morador.getCondominioId(), condominioId, "Morador");

        Vaga vaga = buscarVagaAtiva(dto.getVagaId());
        validarVagaMesmoCondo(vaga, condominioId);
        validarMesmoApartamento(morador, vaga);

        veiculo.setProprietarioId(dto.getProprietarioId());
        veiculo.setVaga(vaga);
    }

    private void configurarVeiculoVisitante(CriarVeiculoDTO dto, Veiculo veiculo,
                                            String condominioId, String perfil, JwtClaims claims) {
        if (!isPorteiro(perfil) && !isAdmin(perfil)) {
            throw new AcessoNegadoException("Apenas porteiros e administradores podem cadastrar veículos de visitante.");
        }
        if (dto.getProprietarioId() == null || dto.getProprietarioId().isBlank()) {
            throw new OperacaoInvalidaException("Morador anfitrião é obrigatório para veículos de Visitante.");
        }

        Morador anfitriao = moradorRepository.findById(dto.getProprietarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado: " + dto.getProprietarioId()));
        validarMesmoCondo(anfitriao.getCondominioId(), condominioId, "Morador anfitrião");

        veiculo.setProprietarioId(dto.getProprietarioId());
        // Vaga NÃO é definida no cadastro — será escolhida na entrada (RN-04)
    }

    private void configurarVeiculoServico(CriarVeiculoDTO dto, Veiculo veiculo, String condominioId) {
        if (dto.getProprietarioId() == null || dto.getProprietarioId().isBlank()) {
            throw new OperacaoInvalidaException("Funcionário vinculado é obrigatório para veículos de Serviço.");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.getProprietarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado: " + dto.getProprietarioId()));
        validarMesmoCondo(funcionario.getCondominioId(), condominioId, "Funcionário");

        veiculo.setProprietarioId(dto.getProprietarioId());
        // Veículos de serviço não têm vaga (RN-02)
    }

    public VeiculoResponseDTO atualizar(String id, CriarVeiculoDTO dto) {
        JwtClaims claims = authRequired();
        Veiculo veiculo = buscarEntidade(id);
        validarPermissaoEdicao(claims, veiculo);

        String placaNormAtualizar = dto.getPlaca().toUpperCase().trim();
        validarFormatoPlaca(placaNormAtualizar);
        validarPlacaUnica(placaNormAtualizar, id);

        veiculo.setPlaca(placaNormAtualizar);
        veiculo.setModelo(dto.getModelo());
        veiculo.setCor(dto.getCor());
        veiculo.setObs(dto.getObs());
        veiculo.setAtualizadoEm(LocalDateTime.now());

        // Atualiza vaga apenas se enviada (e tipo suporta)
        TipoProprietario tipo = veiculo.getTipoProprietario();
        if (tipo == TipoProprietario.MORADOR && dto.getVagaId() != null && !dto.getVagaId().isBlank()) {
            Vaga novaVaga = buscarVagaAtiva(dto.getVagaId());
            if (dto.getProprietarioId() != null && !dto.getProprietarioId().isBlank()) {
                Morador morador = moradorRepository.findByIdComApartamento(dto.getProprietarioId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado: " + dto.getProprietarioId()));
                validarMesmoApartamento(morador, novaVaga);
                veiculo.setProprietarioId(dto.getProprietarioId());
            }
            veiculo.setVaga(novaVaga);
        }

        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    public VeiculoResponseDTO alterarVaga(String veiculoId, AlterarVagaDTO dto) {
        JwtClaims claims = authRequired();
        Veiculo veiculo = buscarEntidade(veiculoId);
        validarPermissaoEdicao(claims, veiculo);

        if (dto.getVagaId() == null || dto.getVagaId().isBlank()) {
            if (!veiculo.isVeiculoServico()) {
                throw new OperacaoInvalidaException("Não é possível desvincular a vaga de um veículo do tipo " + veiculo.getCategoria() + ".");
            }
            veiculo.setVaga(null);
        } else {
            Vaga novaVaga = buscarVagaAtiva(dto.getVagaId());
            if (veiculo.getTipoProprietario() == TipoProprietario.MORADOR) {
                Morador morador = moradorRepository.findByIdComApartamento(veiculo.getProprietarioId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Morador do veículo não encontrado."));
                validarMesmoApartamento(morador, novaVaga);
            }
            veiculo.setVaga(novaVaga);
        }

        veiculo.setAtualizadoEm(LocalDateTime.now());
        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    /**
     * Registra entrada de veículo cadastrado.
     *
     * @param vagaId obrigatório para VISITANTE (vaga escolhida na portaria); ignorado para MORADOR e SERVIÇO.
     */
    public VeiculoResponseDTO registrarEntrada(String id, String vagaId) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a entrada de veículos.");
        }

        Veiculo veiculo = buscarEntidade(id);
        String condominioId = veiculo.getCondominioId();

        // RN-07: bloquear entrada duplicada
        movimentacaoRepository.findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(condominioId, veiculo.getPlaca())
                .ifPresent(mov -> {
                    throw new OperacaoInvalidaException(
                            "O veículo " + veiculo.getPlaca() + " já está no condomínio desde " +
                            mov.getEntradaEm().format(DT_BR) + ".");
                });

        Vaga vagaParaEntrada = null;
        TipoProprietario tipo = veiculo.getTipoProprietario();

        if (tipo == TipoProprietario.FUNCIONARIO) {
            // Serviço: sem vaga
        } else if (tipo == TipoProprietario.VISITANTE) {
            // Visitante: usa vagaId informado, ou a vaga já associada ao veículo (cadastrada via atendimento)
            if (vagaId != null && !vagaId.isBlank()) {
                vagaParaEntrada = buscarVagaAtiva(vagaId);
                verificarVagaLivre(vagaParaEntrada, condominioId);
                veiculo.setVaga(vagaParaEntrada);
            } else if (veiculo.getVaga() != null) {
                vagaParaEntrada = veiculo.getVaga();
                verificarVagaLivre(vagaParaEntrada, condominioId);
            } else {
                throw new OperacaoInvalidaException("Informe a vaga para veículo de Visitante.");
            }
        } else {
            // Morador: vaga já está no cadastro
            if (veiculo.getVaga() == null) {
                throw new OperacaoInvalidaException(
                        "Veículo sem vaga vinculada. Vincule uma vaga antes de registrar a entrada.");
            }
            vagaParaEntrada = veiculo.getVaga();
            // RN-06: bloquear se vaga já ocupada por outro veículo
            verificarVagaLivre(vagaParaEntrada, condominioId);
        }

        LocalDateTime agora = LocalDateTime.now();
        veiculo.setDataEntrada(agora);
        veiculo.setStatus(StatusAcesso.DENTRO);
        veiculo.setAtualizadoEm(agora);
        VeiculoResponseDTO result = VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
        salvarEntradaMovimentacao(veiculo, vagaParaEntrada, claims.authUserId(), agora);
        return result;
    }

    /** Sobrecarga sem vagaId para compatibilidade com o fluxo de MORADOR. */
    public VeiculoResponseDTO registrarEntrada(String id) {
        return registrarEntrada(id, null);
    }

    public VeiculoResponseDTO registrarEntradaPorPlaca(String placa) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a entrada de veículos.");
        }

        Veiculo veiculo = veiculoRepository.findByPlaca(placa.toUpperCase().trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo com placa " + placa + " não encontrado."));
        return registrarEntrada(veiculo.getId());
    }

    public VeiculoResponseDTO registrarSaida(String id) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a saída de veículos.");
        }

        Veiculo veiculo = buscarEntidade(id);
        if (veiculo.getStatus() != StatusAcesso.DENTRO) {
            throw new OperacaoInvalidaException("Veículo não está registrado como DENTRO do condomínio.");
        }

        LocalDateTime agora = LocalDateTime.now();
        veiculo.setDataSaida(agora);
        veiculo.setStatus(StatusAcesso.SAIU);
        veiculo.setAtualizadoEm(agora);

        // AJUSTE 4: veículo de visitante — encerra também a saída do visitante condutor
        String visitanteNomeSaiu = null;
        boolean visitanteSaiu = false;
        if (veiculo.getTipoProprietario() == TipoProprietario.VISITANTE) {
            veiculo.setVaga(null);
            if (veiculo.getProprietarioId() != null) {
                Visitante visitante = visitanteRepository.findById(veiculo.getProprietarioId()).orElse(null);
                if (visitante != null) {
                    visitanteNomeSaiu = visitante.getNome();
                    if (visitante.getStatus() == StatusAcesso.DENTRO) {
                        visitante.setHorarioSaida(agora);
                        visitante.setStatus(StatusAcesso.SAIU);
                        visitante.setAtualizadoEm(agora);
                        visitanteRepository.save(visitante);
                        visitanteSaiu = true;
                    }
                }
            }
        }

        Veiculo saved = veiculoRepository.save(veiculo);
        fecharMovimentacaoAberta(saved.getPlaca(), saved.getCondominioId(), agora, claims.authUserId());
        return VeiculoResponseDTO.builderFromEntity(saved)
                .visitanteNome(visitanteNomeSaiu)
                .visitanteSaiu(visitanteSaiu)
                .build();
    }

    public List<VeiculoResponseDTO> listarTodos() {
        JwtClaims claims = authRequired();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (isMorador(claims.perfil())) {
            return moradorDoClaims(claims)
                    .map(m -> veiculoRepository.findByProprietarioId(m.getId())
                            .stream().map(VeiculoResponseDTO::fromEntity).toList())
                    .orElse(List.of());
        }
        if (condominioId != null) {
            return veiculoRepository.findByCondominioId(condominioId).stream()
                    .map(VeiculoResponseDTO::fromEntity).toList();
        }
        return veiculoRepository.findAll().stream().map(VeiculoResponseDTO::fromEntity).toList();
    }

    public List<VeiculoResponseDTO> listarDentro() {
        JwtClaims claims = authRequired();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (isMorador(claims.perfil())) {
            return moradorDoClaims(claims)
                    .map(m -> veiculoRepository.findByProprietarioId(m.getId())
                            .stream()
                            .filter(v -> v.getStatus() == StatusAcesso.DENTRO)
                            .map(VeiculoResponseDTO::fromEntity).toList())
                    .orElse(List.of());
        }
        if (condominioId != null) {
            return veiculoRepository.findByCondominioIdAndStatus(condominioId, StatusAcesso.DENTRO).stream()
                    .map(VeiculoResponseDTO::fromEntity).toList();
        }
        return veiculoRepository.findByStatus(StatusAcesso.DENTRO).stream().map(VeiculoResponseDTO::fromEntity).toList();
    }

    public List<VeiculoResponseDTO> listarPorProprietario(String proprietarioId) {
        JwtClaims claims = authRequired();
        if (isMorador(claims.perfil())) {
            Morador atual = moradorDoClaims(claims)
                    .orElseThrow(() -> new AcessoNegadoException(
                            "Registro de morador não encontrado para o usuário autenticado."));
            if (!atual.getId().equals(proprietarioId)) {
                throw new AcessoNegadoException("Você só pode consultar seus próprios veículos.");
            }
        }
        return veiculoRepository.findByProprietarioId(proprietarioId).stream()
                .map(VeiculoResponseDTO::fromEntity).toList();
    }

    public VeiculoResponseDTO buscarPorId(String id) {
        authRequired();
        return VeiculoResponseDTO.fromEntity(buscarEntidade(id));
    }

    // ─── Portaria de veículos (MovimentacaoVeiculo) ────────────────────────────

    public MovimentacaoVeiculoResponseDTO registrarEntradaAvulsa(String placa) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a entrada de veículos.");
        }
        String placaNorm = placa.toUpperCase().trim();
        validarFormatoPlaca(placaNorm);

        String condominioId = CondominioUtils.condominioIdEfetivo();
        movimentacaoRepository.findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(condominioId, placaNorm)
                .ifPresent(mov -> {
                    throw new OperacaoInvalidaException(
                            "O veículo " + placaNorm + " já está no condomínio desde " +
                            mov.getEntradaEm().format(DT_BR) + ".");
                });

        MovimentacaoVeiculo mov = new MovimentacaoVeiculo();
        mov.setPlaca(placaNorm);
        mov.setCondominioId(condominioId);
        mov.setRegistradoPorEntradaId(claims.authUserId());
        mov.setEntradaEm(LocalDateTime.now());
        return MovimentacaoVeiculoResponseDTO.fromEntity(movimentacaoRepository.save(mov));
    }

    public MovimentacaoVeiculoResponseDTO registrarSaidaPorPlaca(String placa) {
        JwtClaims claims = authRequired();
        if (!isAdmin(claims.perfil()) && !isPorteiro(claims.perfil())) {
            throw new AcessoNegadoException("Apenas porteiros podem registrar a saída de veículos.");
        }
        String placaNorm = placa.toUpperCase().trim();
        String condominioId = CondominioUtils.condominioIdEfetivo();

        MovimentacaoVeiculo mov = movimentacaoRepository
                .findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(condominioId, placaNorm)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma entrada em aberto para o veículo " + placaNorm + "."));

        LocalDateTime agora = LocalDateTime.now();
        mov.setSaidaEm(agora);
        mov.setRegistradoPorSaidaId(claims.authUserId());
        movimentacaoRepository.save(mov);

        if (mov.getVeiculoId() != null) {
            veiculoRepository.findById(mov.getVeiculoId()).ifPresent(v -> {
                v.setDataSaida(agora);
                v.setStatus(StatusAcesso.SAIU);
                if (v.getTipoProprietario() == TipoProprietario.VISITANTE) {
                    v.setVaga(null);
                }
                v.setAtualizadoEm(agora);
                veiculoRepository.save(v);
            });
        }

        return MovimentacaoVeiculoResponseDTO.fromEntity(mov);
    }

    public List<MovimentacaoVeiculoResponseDTO> listarDentroPortaria() {
        authRequired();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        return movimentacaoRepository
                .findByCondominioIdAndSaidaEmIsNullOrderByEntradaEmDesc(condominioId)
                .stream()
                .map(MovimentacaoVeiculoResponseDTO::fromEntity)
                .toList();
    }

    public List<MovimentacaoVeiculoResponseDTO> listarHistoricoAcesso(
            String placa, String dataInicioStr, String dataFimStr, String status) {
        authRequired();
        String condominioId = CondominioUtils.condominioIdEfetivo();

        LocalDate dataInicio = (dataInicioStr != null && !dataInicioStr.isBlank())
                ? LocalDate.parse(dataInicioStr) : LocalDate.now();
        LocalDate dataFim = (dataFimStr != null && !dataFimStr.isBlank())
                ? LocalDate.parse(dataFimStr) : LocalDate.now();

        String placaPattern = (placa != null && !placa.isBlank())
                ? "%" + placa.toUpperCase().trim() + "%" : null;
        String statusParam = (status != null && !status.isBlank()) ? status : null;

        return movimentacaoRepository.buscarHistoricoFiltrado(
                condominioId,
                dataInicio.atStartOfDay(),
                dataFim.atTime(23, 59, 59),
                placaPattern,
                statusParam
        ).stream().map(MovimentacaoVeiculoResponseDTO::fromEntity).toList();
    }

    // ─── Helpers internos ──────────────────────────────────────────────────────

    private void validarFormatoPlaca(String placa) {
        if (!PLACA_PATTERN.matcher(placa).matches()) {
            throw new OperacaoInvalidaException(
                    "Placa inválida: " + placa + ". Use AAA9999 (formato antigo) ou AAA9A99 (Mercosul).");
        }
    }

    private void verificarVagaLivre(Vaga vaga, String condominioId) {
        if (movimentacaoRepository.existsByCondominioIdAndVagaIdAndSaidaEmIsNull(condominioId, vaga.getId())) {
            // Encontra o veículo que está ocupando para incluir na mensagem
            movimentacaoRepository.findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(condominioId, vaga.getId());
            // Busca movimentação aberta desta vaga para pegar a placa
            var aberta = movimentacaoRepository
                    .findByCondominioIdAndSaidaEmIsNullOrderByEntradaEmDesc(condominioId)
                    .stream()
                    .filter(m -> vaga.getId().equals(m.getVagaId()))
                    .findFirst();
            String msg = aberta.map(m -> "A vaga " + vaga.getNumero() + " já está ocupada pelo veículo " +
                    m.getPlaca() + " desde " + m.getEntradaEm().format(DT_BR) + ".")
                    .orElse("A vaga " + vaga.getNumero() + " já está ocupada.");
            throw new OperacaoInvalidaException(msg);
        }
    }

    private void salvarEntradaMovimentacao(Veiculo veiculo, Vaga vaga, String registradoPorId, LocalDateTime entradaEm) {
        String vinculadoNome = veiculo.getProprietarioId() != null
                ? moradorRepository.findById(veiculo.getProprietarioId())
                        .map(Morador::getNome).orElse(null)
                : null;
        MovimentacaoVeiculo mov = new MovimentacaoVeiculo();
        mov.setPlaca(veiculo.getPlaca());
        mov.setModelo(veiculo.getModelo());
        mov.setVeiculoId(veiculo.getId());
        mov.setVagaId(vaga != null ? vaga.getId() : null);
        mov.setCondominioId(veiculo.getCondominioId());
        mov.setVinculadoNome(vinculadoNome);
        mov.setRegistradoPorEntradaId(registradoPorId);
        mov.setEntradaEm(entradaEm);
        movimentacaoRepository.save(mov);
    }

    private void fecharMovimentacaoAberta(String placa, String condominioId, LocalDateTime saidaEm, String registradoPorId) {
        movimentacaoRepository.findFirstByCondominioIdAndPlacaAndSaidaEmIsNull(condominioId, placa)
                .ifPresent(mov -> {
                    mov.setSaidaEm(saidaEm);
                    mov.setRegistradoPorSaidaId(registradoPorId);
                    movimentacaoRepository.save(mov);
                });
    }

    private Veiculo buscarEntidade(String id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com id: " + id));
    }

    private Vaga buscarVagaAtiva(String vagaId) {
        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga não encontrada: " + vagaId));
        if (!vaga.isAtiva()) {
            throw new OperacaoInvalidaException("A vaga " + vaga.getNumero() + " está desativada.");
        }
        return vaga;
    }

    private void validarMesmoApartamento(Morador morador, Vaga vaga) {
        if (morador.getApartamento() == null) {
            throw new OperacaoInvalidaException("O morador não está vinculado a nenhum apartamento.");
        }
        if (vaga.getApartamento() == null) {
            throw new OperacaoInvalidaException("A vaga não está vinculada a nenhum apartamento.");
        }
        if (!morador.getApartamento().getId().equals(vaga.getApartamento().getId())) {
            throw new OperacaoInvalidaException(
                    "A vaga selecionada pertence a um apartamento diferente do proprietário do veículo.");
        }
    }

    private void validarMesmoCondo(String condoEntidade, String condoAtual, String tipoEntidade) {
        if (condoAtual != null && !condoAtual.equals(condoEntidade)) {
            throw new AcessoNegadoException(tipoEntidade + " pertence a outro condomínio.");
        }
    }

    private void validarVagaMesmoCondo(Vaga vaga, String condominioId) {
        if (condominioId != null && !condominioId.equals(vaga.getCondominioId())) {
            throw new AcessoNegadoException("Vaga pertence a outro condomínio.");
        }
    }

    private void validarPlacaUnica(String placa, String veiculoIdAtual) {
        veiculoRepository.findByPlaca(placa.toUpperCase().trim()).ifPresent(v -> {
            if (!v.getId().equals(veiculoIdAtual)) {
                throw new OperacaoInvalidaException("Já existe um veículo cadastrado com a placa: " + placa);
            }
        });
    }
}
