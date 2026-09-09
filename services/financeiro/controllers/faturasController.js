import * as faturasModel from '../models/faturasModel.js';
import * as cobrancasModel from '../models/cobrancasModel.js';
import * as faturamentoService from '../services/faturamentoService.js';
import * as asaasClient from '../gateway/asaasClient.js';
import { identidadeDoAtor } from '../clients/authClient.js';
import { emTransacao, consultar } from '../config/database.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

// ── MORADOR ──────────────────────────────────────────────────────────────────

/** Lista as faturas da própria unidade do morador. */
export async function minhasFaturas(req, res) {
  const { condominioId, unidadeId } = req.escopo;
  const faturas = await faturasModel.listarPorUnidade(condominioId, unidadeId);
  res.json({ sucesso: true, total: faturas.length, faturas });
}

/**
 * Detalhe de uma fatura com itens e dados de pagamento.
 *
 * Se ainda não há cobrança no Asaas para esta fatura, cria uma on-demand
 * usando o CPF do próprio usuário autenticado.
 */
export async function detalhesFatura(req, res) {
  const { condominioId, unidadeId } = req.escopo;
  const fatura = await faturasModel.porId(condominioId, req.params.id);

  if (!fatura || fatura.unidadeId !== unidadeId) {
    return res.status(404).json({ sucesso: false, mensagem: 'Fatura não encontrada.' });
  }

  const [itens, cobranca] = await Promise.all([
    faturasModel.listarItens(fatura.id),
    cobrancasModel.porFatura(fatura.id),
  ]);

  res.json({ sucesso: true, fatura, itens, cobranca: cobranca ?? null });
}

/**
 * Gera (ou reutiliza) a cobrança no gateway para a fatura do morador.
 *
 * Se já existe uma cobrança ativa para esta fatura, retorna a existente sem
 * criar outra. Isso garante idempotência: o morador pode trocar de aba e
 * voltar sem gerar múltiplas cobranças no Asaas.
 *
 * Aceita `forma: 'PIX' | 'BOLETO'` no body.
 */
export async function gerarCobranca(req, res) {
  const { condominioId, unidadeId } = req.escopo;
  const fatura = await faturasModel.porId(condominioId, req.params.id);

  if (!fatura || fatura.unidadeId !== unidadeId) {
    return res.status(404).json({ sucesso: false, mensagem: 'Fatura não encontrada.' });
  }

  if (!['ABERTA', 'EM_ATRASO'].includes(fatura.status)) {
    return res.status(409).json({ sucesso: false, mensagem: `Fatura com status ${fatura.status} não pode ser paga.` });
  }

  const forma = String(req.body?.forma ?? 'PIX').toUpperCase();
  if (!['PIX', 'BOLETO'].includes(forma)) {
    return res.status(400).json({ sucesso: false, mensagem: 'Forma inválida. Use PIX ou BOLETO.' });
  }

  // Reutiliza cobrança existente da mesma forma, se disponível.
  const existente = await cobrancasModel.porFaturaEForma(fatura.id, forma);
  if (existente) {
    return res.json({ sucesso: true, cobranca: existente });
  }

  // Precisa do CPF do morador para criar cliente no gateway.
  const identidade = await identidadeDoAtor(req.claims.id, req.authorization);
  if (!identidade?.cpf) {
    return res.status(400).json({
      sucesso: false,
      mensagem: 'CPF não cadastrado. Atualize seu perfil para habilitar o pagamento online.',
    });
  }

  const cliente = await asaasClient.garantirCliente({
    nome: identidade.nome ?? 'Morador',
    cpf: identidade.cpf,
    email: identidade.email,
    usuarioId: req.claims.id,
  });

  if (!cliente.ok) {
    console.warn('[financeiro] garantirCliente falhou:', cliente.erro);
    return res.status(502).json({ sucesso: false, mensagem: 'Não foi possível registrar seus dados no gateway. Tente novamente.' });
  }

  const cobr = await asaasClient.criarCobranca({
    clienteId: cliente.dados.id,
    forma,
    valorCentavos: fatura.valorCentavos,
    vencimento: fatura.vencimento,
    descricao: `Condominio - ${String(fatura.competencia).slice(0, 7)}`,
    faturaId: fatura.id,
  });

  if (!cobr.ok) {
    console.warn('[financeiro] criarCobranca falhou:', cobr.erro);
    return res.status(502).json({ sucesso: false, mensagem: 'Não foi possível gerar a cobrança. Tente novamente.' });
  }

  let pixPayload = null;
  let pixQrcode = null;
  let urlBoleto = null;

  if (forma === 'PIX') {
    const pix = await asaasClient.buscarPixQrCode(cobr.dados.id);
    if (pix.ok) {
      pixPayload = pix.dados?.payload ?? null;
      pixQrcode = pix.dados?.encodedImage ?? null;
    }
  } else {
    const linha = await asaasClient.buscarLinhaDigitavel(cobr.dados.id);
    urlBoleto = cobr.dados.bankSlipUrl ?? null;
    if (linha.ok) pixPayload = linha.dados?.identificationField ?? null;
  }

  const cobranca = await cobrancasModel.criar({
    faturaId: fatura.id,
    condominioId,
    asaasId: cobr.dados.id,
    billingType: forma,
    valorCentavos: fatura.valorCentavos,
    vencimento: fatura.vencimento,
    urlBoleto,
    pixPayload,
    pixQrcode,
  });

  res.status(201).json({ sucesso: true, cobranca });
}

/** Gastos detalhados da unidade — itens de fatura com contexto de competência. */
export async function meuGastos(req, res) {
  const { condominioId, unidadeId } = req.escopo;
  const ano = req.query.ano ? Number(req.query.ano) : null;
  const itens = await faturasModel.listarItensUnidade(condominioId, unidadeId, ano);
  res.json({ sucesso: true, total: itens.length, itens });
}

// ── SÍNDICO ──────────────────────────────────────────────────────────────────

/** Lista faturas do condomínio com filtros. */
export async function listarFaturas(req, res) {
  const { condominioId } = req.escopo;
  const { competencia, status, unidadeId } = req.query;
  const faturas = await faturasModel.listarPorCondominio(condominioId, {
    competencia: competencia ? (competencia.length === 7 ? competencia + '-01' : competencia) : null,
    status: status || null,
    unidadeId: unidadeId || null,
  });
  res.json({ sucesso: true, total: faturas.length, faturas });
}

/** KPIs de uma competência. */
export async function kpis(req, res) {
  const { condominioId } = req.escopo;
  const comp = req.query.competencia;
  if (!comp) return res.status(400).json({ sucesso: false, mensagem: 'Informe a competência.' });
  const competencia = comp.length === 7 ? comp + '-01' : comp;
  const kpi = await faturasModel.kpisPorCompetencia(condominioId, competencia);
  res.json({ sucesso: true, ...kpi });
}

/** Preview do fechamento de competência (não grava nada). */
export async function preview(req, res) {
  const { condominioId } = req.escopo;
  const competencia = req.query.competencia;
  if (!competencia) return res.status(400).json({ sucesso: false, mensagem: 'Informe a competência.' });
  const r = await faturamentoService.previewCompetencia(condominioId, competencia, req.authorization);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
}

/** Executa o fechamento de competência. */
export async function fecharCompetencia(req, res) {
  const { condominioId } = req.escopo;
  const r = await faturamentoService.fecharCompetencia(
    condominioId, req.body?.competencia, req.authorization,
  );
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
}

/** Baixa manual de uma fatura (síndico marca como paga sem gateway). */
export async function baixaManual(req, res) {
  const { condominioId } = req.escopo;
  const fatura = await faturasModel.porId(condominioId, req.params.id);
  if (!fatura) return res.status(404).json({ sucesso: false, mensagem: 'Fatura não encontrada.' });
  if (!['ABERTA', 'EM_ATRASO'].includes(fatura.status)) {
    return res.status(409).json({ sucesso: false, mensagem: `Fatura não pode ser baixada com status ${fatura.status}.` });
  }

  const pagoEm = req.body.pagoEm ?? new Date().toISOString();
  const valorPagoCentavos = req.body.valorCentavos ?? fatura.valorCentavos;

  const atualizada = await emTransacao(async (cliente) =>
    faturasModel.marcarPaga(cliente, fatura.id, {
      pagoEm,
      valorPagoCentavos,
      formaBaixa: 'MANUAL',
    }),
  );

  if (!atualizada) return res.status(409).json({ sucesso: false, mensagem: 'Não foi possível baixar a fatura.' });
  res.json({ sucesso: true, fatura: atualizada });
}

// ── ADMIN GERAL ───────────────────────────────────────────────────────────────

/** Resumo de taxa da plataforma por mês — visão do Admin Geral. */
export async function taxaPlataformaAdmin(req, res) {
  const mes = req.query.mes;
  if (!mes) return res.status(400).json({ sucesso: false, mensagem: 'Informe o mês (YYYY-MM).' });
  const competencia = mes.length === 7 ? mes + '-01' : mes;

  const { rows } = await consultar(
    `SELECT
       f.condominio_id AS "condominioId",
       COUNT(*) AS faturas,
       COALESCE(SUM(fi.valor_centavos), 0) AS "totalCentavos",
       COALESCE(SUM(fi.valor_centavos) FILTER (WHERE f.status = 'PAGA'), 0) AS "pagoCentavos"
     FROM fatura_itens fi
     JOIN faturas f ON f.id = fi.fatura_id
     WHERE fi.tipo = 'TAXA_PLATAFORMA' AND f.competencia = $1
     GROUP BY f.condominio_id
     ORDER BY f.condominio_id`,
    [competencia],
  );

  res.json({ sucesso: true, competencia, condominios: rows });
}
