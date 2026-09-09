import * as tiposTaxa from '../models/tiposTaxaModel.js';
import * as regras from '../models/regrasTaxaModel.js';
import { paraCentavos } from '../utils/dinheiro.js';

const BASES = ['POR_UNIDADE', 'TOTAL_CONDOMINIO'];
const PERIODICIDADES = ['MENSAL', 'ANUAL', 'UNICA'];
const MODOS = ['FIXO', 'FRACAO_IDEAL'];

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

/** Valida e normaliza o corpo de um tipo de taxa. */
function normalizarTipo(corpo) {
  const nome = String(corpo.nome ?? '').trim();
  if (nome.length < 2) return erro('Informe o nome da taxa.');
  if (nome.length > 120) return erro('O nome da taxa é longo demais.');

  const valorCentavos = paraCentavos(corpo.valor ?? corpo.valorCentavos);
  if (valorCentavos === null) return erro('Informe um valor válido, como 350,00.');
  if (valorCentavos < 0) return erro('O valor não pode ser negativo.');

  const baseCalculo = corpo.baseCalculo ?? 'POR_UNIDADE';
  if (!BASES.includes(baseCalculo)) return erro('Base de cálculo inválida.');

  const periodicidade = corpo.periodicidade ?? 'MENSAL';
  if (!PERIODICIDADES.includes(periodicidade)) return erro('Periodicidade inválida.');

  const parcelas = Number(corpo.parcelas ?? 1);
  if (!Number.isInteger(parcelas) || parcelas < 1) return erro('Parcelas deve ser 1 ou mais.');

  const extraordinaria = Boolean(corpo.extraordinaria);
  // Parcelar uma taxa mensal faria a parcela de um mês colidir com a do
  // seguinte; parcelamento é o que distingue a taxa extraordinária.
  if (parcelas > 1 && !extraordinaria) {
    return erro('Só taxa extraordinária pode ser parcelada.');
  }

  return {
    nome,
    descricao: corpo.descricao?.trim() || null,
    valorCentavos,
    baseCalculo,
    periodicidade,
    extraordinaria,
    parcelas,
    ativo: corpo.ativo ?? true,
  };
}

export async function listarTipos(condominioId, incluirInativos) {
  return tiposTaxa.listar(condominioId, { incluirInativos });
}

export async function criarTipo(condominioId, corpo) {
  const t = normalizarTipo(corpo);
  if (t.erro) return t;
  try {
    return await tiposTaxa.criar(condominioId, t);
  } catch (err) {
    // 23505 = unique_violation; o índice é (condominio_id, nome).
    if (err.code === '23505') return erro('Já existe uma taxa com esse nome.', 409);
    throw err;
  }
}

export async function atualizarTipo(condominioId, id, corpo) {
  const t = normalizarTipo(corpo);
  if (t.erro) return t;
  try {
    const salvo = await tiposTaxa.atualizar(condominioId, id, t);
    // 404, e não 403: dizer "existe mas não é seu" confirmaria a existência da
    // taxa de outro condomínio.
    return salvo ?? erro('Taxa não encontrada.', 404);
  } catch (err) {
    if (err.code === '23505') return erro('Já existe uma taxa com esse nome.', 409);
    throw err;
  }
}

export async function desativarTipo(condominioId, id) {
  const salvo = await tiposTaxa.desativar(condominioId, id);
  return salvo ?? erro('Taxa não encontrada.', 404);
}

export async function obterRegras(condominioId) {
  return regras.obterOuCriar(condominioId);
}

export async function atualizarRegras(condominioId, corpo) {
  const modo = corpo.modo ?? 'FIXO';
  if (!MODOS.includes(modo)) return erro('Modo de rateio inválido.');

  const dia = (valor, campo) => {
    const n = Number(valor);
    // O teto é 28 porque fevereiro existe: dia 30 não cai em todos os meses, e
    // a regra precisa valer para os doze.
    if (!Number.isInteger(n) || n < 1 || n > 28) {
      return erro(`${campo} precisa ser um dia entre 1 e 28.`);
    }
    return n;
  };

  const diaFechamento = dia(corpo.diaFechamento, 'Dia de fechamento');
  if (diaFechamento.erro) return diaFechamento;
  const diaVencimento = dia(corpo.diaVencimento, 'Dia de vencimento');
  if (diaVencimento.erro) return diaVencimento;

  const diasRecursoMulta = Number(corpo.diasRecursoMulta ?? 15);
  if (!Number.isInteger(diasRecursoMulta) || diasRecursoMulta < 0) {
    return erro('Prazo de recurso inválido.');
  }

  await regras.obterOuCriar(condominioId);
  return regras.atualizar(condominioId, {
    modo, diaFechamento, diaVencimento, diasRecursoMulta,
    incluirTaxaPlataforma: corpo.incluirTaxaPlataforma ?? null,
  });
}
