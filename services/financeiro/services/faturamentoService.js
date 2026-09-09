import * as regrasTaxaModel from '../models/regrasTaxaModel.js';
import * as tiposTaxaModel from '../models/tiposTaxaModel.js';
import * as contasConsumoModel from '../models/contasConsumoModel.js';
import * as fracaoModel from '../models/fracaoModel.js';
import * as faturasModel from '../models/faturasModel.js';
import * as notificacaoService from './notificacaoService.js';
import * as emailService from './emailFinanceiroService.js';
import { listarUnidades } from '../clients/portariaClient.js';
import { listarResidentesPorUnidade } from '../clients/authClient.js';
import { buscarTaxaPlataforma } from '../utils/planClient.js';
import { formatarBRL } from '../utils/dinheiro.js';

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

/**
 * Calcula a data de vencimento da competência.
 *
 * Se diaVencimento > diaFechamento o vencimento cai no mesmo mês (ex: fecha
 * dia 25, vence dia 10 do mês seguinte). Caso contrário, vence no mês da
 * competência (raro, mas possível quando o condomínio aceita pagamento
 * antecipado).
 */
function calcularVencimento(competenciaDate, diaFechamento, diaVencimento) {
  const [ano, mes] = competenciaDate.split('-').map(Number);
  const venceProximo = diaVencimento >= diaFechamento;
  const mesVenc = venceProximo ? mes + 1 : mes;
  const anoVenc = mesVenc > 12 ? ano + 1 : ano;
  const mesVencNorm = mesVenc > 12 ? 1 : mesVenc;
  return `${anoVenc}-${String(mesVencNorm).padStart(2, '0')}-${String(diaVencimento).padStart(2, '0')}`;
}

function competenciaLabel(competenciaDate) {
  const [ano, mes] = competenciaDate.split('-');
  const meses = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun',
    'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  return `${meses[Number(mes) - 1]}/${ano}`;
}

/**
 * Fecha a competência de um condomínio: gera uma fatura por unidade,
 * composta por taxas recorrentes + contas de consumo + taxa da plataforma.
 *
 * Idempotente: se a fatura da unidade já existe (e não está cancelada),
 * a unidade é marcada como "pulada" no resultado sem erro.
 *
 * Não cria cobranças no Asaas — isso acontece no momento em que o morador
 * aciona o pagamento, usando as próprias credenciais.
 */
export async function fecharCompetencia(condominioId, competenciaInput, authorization) {
  if (!competenciaInput) return erro('Informe a competência no formato YYYY-MM.');

  const competencia = competenciaInput.length === 7
    ? competenciaInput + '-01'
    : competenciaInput;

  if (!/^\d{4}-\d{2}-01$/.test(competencia)) {
    return erro('Competência inválida. Use o formato YYYY-MM.');
  }

  const regras = await regrasTaxaModel.obterOuCriar(condominioId);
  const vencimento = calcularVencimento(competencia, regras.diaFechamento, regras.diaVencimento);
  const label = competenciaLabel(competencia);

  const [taxas, contas, unidadesR, fracoesAll] = await Promise.all([
    tiposTaxaModel.listar(condominioId, { incluirInativos: false }),
    contasConsumoModel.listarParaFaturamento(condominioId, competencia),
    listarUnidades(condominioId, authorization),
    fracaoModel.listarPorCondominio(condominioId),
  ]);

  if (!unidadesR.ok) {
    return erro('Não foi possível obter as unidades do condomínio. Tente novamente.', 503);
  }
  const unidades = unidadesR.dados;
  if (!unidades.length) return erro('Nenhuma unidade encontrada no condomínio.');

  const fracaoMap = new Map(fracoesAll.map((f) => [f.unidadeId, f.milesimos]));
  const totalMilesimos = fracoesAll.reduce((s, f) => s + f.milesimos, 0);

  let taxaPlataformaCentavos = 0;
  if (regras.incluirTaxaPlataforma) {
    const plan = await buscarTaxaPlataforma(condominioId, authorization);
    taxaPlataformaCentavos = plan.mensalidadeCentavos ?? 0;
  }

  const resultados = [];

  for (const unidade of unidades) {
    const fracaoMilesimos = fracaoMap.get(unidade.id) ?? 0;

    // Em modo FRACAO_IDEAL só fatura unidades com fração configurada.
    if (regras.modo === 'FRACAO_IDEAL' && fracaoMilesimos === 0) {
      resultados.push({ unidadeId: unidade.id, status: 'sem_fracao' });
      continue;
    }

    const peso = regras.modo === 'FRACAO_IDEAL' ? fracaoMilesimos : 1;
    const divisor = regras.modo === 'FRACAO_IDEAL' ? totalMilesimos : unidades.length;

    const itens = [];

    for (const taxa of taxas) {
      const valor = taxa.baseCalculo === 'POR_UNIDADE'
        ? taxa.valorCentavos
        : Math.floor((taxa.valorCentavos * peso) / divisor);
      if (valor > 0) itens.push({ tipo: 'TAXA', descricao: taxa.nome, valorCentavos: valor });
    }

    for (const conta of contas) {
      const pesos = conta.modoRateio === 'FRACAO_IDEAL' ? fracaoMilesimos : 1;
      const div = conta.modoRateio === 'FRACAO_IDEAL' ? totalMilesimos : unidades.length;
      const valor = Math.floor((conta.valorTotalCentavos * pesos) / div);
      if (valor > 0) {
        itens.push({
          tipo: 'CONTA_CONSUMO',
          descricao: `${conta.tipo}${conta.descricao ? ': ' + conta.descricao : ''}`,
          valorCentavos: valor,
        });
      }
    }

    if (taxaPlataformaCentavos > 0) {
      const valor = Math.floor((taxaPlataformaCentavos * peso) / divisor);
      if (valor > 0) itens.push({ tipo: 'TAXA_PLATAFORMA', descricao: 'Plataforma Mora', valorCentavos: valor });
    }

    const total = itens.reduce((s, i) => s + i.valorCentavos, 0);
    if (total <= 0) {
      resultados.push({ unidadeId: unidade.id, status: 'sem_valor' });
      continue;
    }

    // Resolve responsável financeiro da unidade para notificação.
    let responsavelId = null;
    let responsavelEmail = null;
    try {
      const r = await listarResidentesPorUnidade(unidade.id, authorization);
      if (r.ok) {
        const resp = r.dados?.moradores?.find((m) => m.responsavelFinanceiro);
        if (resp) {
          responsavelId = resp.id;
          responsavelEmail = resp.email;
        }
      }
    } catch { /* não-fatal */ }

    try {
      const { fatura, criada } = await faturasModel.criarOuObter(
        condominioId, unidade.id, competencia,
        { vencimento, valorCentavos: total, responsavelUsuarioId: responsavelId },
      );

      if (!fatura || !criada) {
        resultados.push({ unidadeId: unidade.id, status: 'existente', faturaId: fatura?.id });
        continue;
      }

      await faturasModel.criarItens(fatura.id, itens);

      // Notificação in-app
      await notificacaoService.criar(
        responsavelId, condominioId, 'NOVA_FATURA',
        'Nova cobrança disponível',
        `Sua fatura de ${label} está disponível. Valor: ${formatarBRL(total)}.`,
        { faturaId: fatura.id },
      );

      // E-mail assíncrono (não aguarda — falha não deve travar o fechamento)
      if (responsavelEmail) {
        emailService.novaFatura(responsavelEmail, {
          nomeUnidade: unidade.numero ?? unidade.id,
          competencia: label,
          valorBRL: formatarBRL(total),
          vencimento,
        }).catch(() => {});
      }

      resultados.push({ unidadeId: unidade.id, status: 'ok', faturaId: fatura.id });
    } catch (err) {
      console.error(`[faturamento] erro na unidade ${unidade.id}:`, err.message);
      resultados.push({ unidadeId: unidade.id, status: 'erro', erro: err.message });
    }
  }

  // Marcar contas de consumo como rateadas
  for (const conta of contas) {
    await contasConsumoModel.marcarRateada(conta.id);
  }

  const criadas = resultados.filter((r) => r.status === 'ok').length;
  const existentes = resultados.filter((r) => r.status === 'existente').length;
  const erros = resultados.filter((r) => r.status === 'erro').length;

  return {
    ok: true,
    competencia,
    unidades: unidades.length,
    faturasCriadas: criadas,
    faturasExistentes: existentes,
    erros,
    detalhe: resultados,
  };
}

/**
 * Preflight — retorna um preview do que seria faturado para a competência.
 * Não grava nada; só informa o síndico antes de confirmar.
 */
export async function previewCompetencia(condominioId, competenciaInput, authorization) {
  const competencia = competenciaInput?.length === 7
    ? competenciaInput + '-01'
    : competenciaInput;

  const [regras, taxas, contas, unidadesR, fracoesAll] = await Promise.all([
    regrasTaxaModel.obterOuCriar(condominioId),
    tiposTaxaModel.listar(condominioId, { incluirInativos: false }),
    contasConsumoModel.listarParaFaturamento(condominioId, competencia),
    listarUnidades(condominioId, authorization),
    fracaoModel.listarPorCondominio(condominioId),
  ]);

  if (!unidadesR.ok) return erro('Não foi possível obter as unidades.', 503);

  const unidades = unidadesR.dados;
  const totalMilesimos = fracoesAll.reduce((s, f) => s + f.milesimos, 0);
  const faturasExistentes = await faturasModel.listarPorCondominio(condominioId, {
    competencia,
  });
  const existentesSet = new Set(faturasExistentes.map((f) => f.unidadeId));

  return {
    ok: true,
    unidades: unidades.length,
    faturasNovas: unidades.filter((u) => !existentesSet.has(u.id)).length,
    faturasExistentes: existentesSet.size,
    taxas: taxas.length,
    contasDeConsumo: contas.length,
    modoRateio: regras.modo,
    fracoesCadastradas: fracoesAll.length,
    totalMilesimos,
  };
}
