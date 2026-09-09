import * as model from '../models/contasConsumoModel.js';
import * as fracaoModel from '../models/fracaoModel.js';
import * as faturasModel from '../models/faturasModel.js';
import { paraCentavos } from '../utils/dinheiro.js';

const TIPOS = ['AGUA', 'LUZ', 'GAS', 'INTERNET', 'OUTRO'];
const MODOS_RATEIO = ['FRACAO_IDEAL', 'FIXO_POR_UNIDADE'];

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

function normalizarConta(corpo) {
  const tipo = String(corpo.tipo ?? '').toUpperCase();
  if (!TIPOS.includes(tipo)) return erro(`Tipo inválido. Use: ${TIPOS.join(', ')}.`);

  const valorTotalCentavos = paraCentavos(corpo.valor ?? corpo.valorTotalCentavos);
  if (!valorTotalCentavos || valorTotalCentavos <= 0) {
    return erro('Informe um valor total válido e maior que zero.');
  }

  const modoRateio = corpo.modoRateio ?? 'FRACAO_IDEAL';
  if (!MODOS_RATEIO.includes(modoRateio)) return erro('Modo de rateio inválido.');

  if (!corpo.vencimento) return erro('Informe a data de vencimento.');
  if (!corpo.competencia) return erro('Informe a competência (YYYY-MM).');

  // Normaliza competencia "YYYY-MM" para "YYYY-MM-01"
  const competencia = corpo.competencia.length === 7
    ? corpo.competencia + '-01'
    : corpo.competencia;

  if (!/^\d{4}-\d{2}-01$/.test(competencia)) {
    return erro('Competência inválida. Use o formato YYYY-MM.');
  }

  return {
    tipo,
    descricao: corpo.descricao?.trim() || null,
    competencia,
    valorTotalCentavos,
    modoRateio,
    vencimento: corpo.vencimento,
    comprovanteUrl: corpo.comprovanteUrl || null,
  };
}

export async function listar(condominioId, competencia) {
  const comp = competencia?.length === 7 ? competencia + '-01' : competencia;
  return model.listar(condominioId, comp || null);
}

export async function criar(condominioId, corpo, criadoPorId) {
  const d = normalizarConta(corpo);
  if (d.erro) return d;
  return model.criar(condominioId, { ...d, criadoPorId });
}

export async function atualizar(condominioId, id, corpo) {
  const existente = await model.porId(condominioId, id);
  if (!existente) return erro('Conta não encontrada.', 404);
  if (existente.status !== 'PENDENTE') return erro('Só é possível editar contas com status PENDENTE.', 409);

  const d = normalizarConta(corpo);
  if (d.erro) return d;

  const salvo = await model.atualizar(condominioId, id, d);
  return salvo ?? erro('Conta não encontrada.', 404);
}

export async function cancelar(condominioId, id) {
  const salvo = await model.cancelar(condominioId, id);
  return salvo ?? erro('Conta não encontrada ou já cancelada.', 404);
}

/**
 * Distribui a conta entre as faturas do mês, criando fatura_itens.
 *
 * Apenas aplica o rateio — não cria faturas novas. Se uma unidade ainda não
 * tem fatura na competência, o item é ignorado para ela (o fechamento de
 * competência cria as faturas; ratear pode rodar antes ou depois).
 *
 * Estratégia deliberada: ratear antecipado permite conferir o valor por unidade
 * antes de emitir as cobranças no gateway.
 */
export async function ratear(condominioId, id) {
  const conta = await model.porId(condominioId, id);
  if (!conta) return erro('Conta não encontrada.', 404);
  if (conta.status === 'CANCELADA') return erro('Conta cancelada não pode ser rateada.', 409);
  if (conta.status === 'RATEADA') return erro('Conta já foi rateada.', 409);

  // Faturas abertas da competência — são o alvo do rateio antecipado.
  const faturas = await faturasModel.listarPorCondominio(condominioId, {
    competencia: conta.competencia,
    status: 'ABERTA',
  });

  if (!faturas.length) {
    // Sem faturas ainda: mantém PENDENTE para o fecharCompetencia incluir a conta.
    return { ok: true, contaId: id, unidadesRateadas: 0, aviso: 'Nenhuma fatura aberta encontrada. A conta será incluída no próximo fechamento.' };
  }

  let inseridos = 0;
  const descricao = `${conta.tipo}${conta.descricao ? ': ' + conta.descricao : ''}`;

  if (conta.modoRateio === 'FRACAO_IDEAL') {
    const fracoes = await fracaoModel.listarPorCondominio(condominioId);
    if (!fracoes.length) return erro('Nenhuma fração ideal cadastrada. Configure as frações antes de ratear no modo proporcional.');
    const totalMilesimos = fracoes.reduce((s, f) => s + f.milesimos, 0);
    if (totalMilesimos <= 0) return erro('Frações somam zero. Verifique a configuração.');

    const faturaMap = new Map(faturas.map((f) => [f.unidadeId, f]));
    for (const fracao of fracoes) {
      const fatura = faturaMap.get(fracao.unidadeId);
      if (!fatura) continue;
      const valorItem = Math.floor((conta.valorTotalCentavos * fracao.milesimos) / totalMilesimos);
      if (valorItem > 0) {
        await faturasModel.criarItens(fatura.id, [{ tipo: 'CONTA_CONSUMO', descricao, valorCentavos: valorItem }]);
        inseridos++;
      }
    }
  } else {
    // FIXO_POR_UNIDADE — divide igualmente entre todas as faturas abertas.
    const valorItem = Math.floor(conta.valorTotalCentavos / faturas.length);
    if (valorItem <= 0) return erro('Valor por unidade resulta em zero após divisão.');
    for (const fatura of faturas) {
      await faturasModel.criarItens(fatura.id, [{ tipo: 'CONTA_CONSUMO', descricao, valorCentavos: valorItem }]);
      inseridos++;
    }
  }

  if (inseridos > 0) await model.marcarRateada(id);

  return { ok: true, contaId: id, unidadesRateadas: inseridos };
}
