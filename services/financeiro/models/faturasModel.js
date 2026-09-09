import { consultar, emTransacao } from '../config/database.js';

const CAMPOS_FATURA = `id, condominio_id AS "condominioId", unidade_id AS "unidadeId",
  competencia, vencimento, valor_centavos AS "valorCentavos", status,
  responsavel_usuario_id AS "responsavelUsuarioId",
  pago_em AS "pagoEm", valor_pago_centavos AS "valorPagoCentavos",
  forma_baixa AS "formaBaixa",
  criado_em AS "criadoEm", atualizado_em AS "atualizadoEm"`;

/**
 * Cria fatura para (condominio, unidade, competencia). Se já existir uma ativa
 * (não cancelada) devolve a existente sem criar. Retorna `{ fatura, criada }`.
 */
export async function criarOuObter(condominioId, unidadeId, competencia, dados) {
  try {
    const { rows } = await consultar(
      `INSERT INTO faturas
         (condominio_id, unidade_id, competencia, vencimento, valor_centavos, responsavel_usuario_id)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING ${CAMPOS_FATURA}`,
      [condominioId, unidadeId, competencia, dados.vencimento,
        dados.valorCentavos, dados.responsavelUsuarioId ?? null],
    );
    return { fatura: rows[0], criada: true };
  } catch (err) {
    if (err.code !== '23505') throw err;
    const { rows } = await consultar(
      `SELECT ${CAMPOS_FATURA} FROM faturas
        WHERE condominio_id = $1 AND unidade_id = $2 AND competencia = $3
          AND status <> 'CANCELADA'`,
      [condominioId, unidadeId, competencia],
    );
    return { fatura: rows[0] ?? null, criada: false };
  }
}

/** Insere os itens de uma fatura em lote. */
export async function criarItens(faturaId, itens) {
  if (!itens.length) return;
  const params = [];
  const values = itens.map((it, i) => {
    const base = i * 4;
    params.push(faturaId, it.tipo, it.descricao, it.valorCentavos);
    // origemId é opcional — não entra como parâmetro para não depender de UUID extra
    return `($${base + 1}, $${base + 2}, $${base + 3}, $${base + 4})`;
  });
  await consultar(
    `INSERT INTO fatura_itens (fatura_id, tipo, descricao, valor_centavos) VALUES ${values.join(', ')}`,
    params,
  );
}

/** Itens de uma fatura. */
export async function listarItens(faturaId) {
  const { rows } = await consultar(
    `SELECT id, tipo, descricao, valor_centavos AS "valorCentavos", origem_id AS "origemId"
       FROM fatura_itens WHERE fatura_id = $1 ORDER BY criado_em`,
    [faturaId],
  );
  return rows;
}

/** Faturas da unidade de um morador — ordenadas das mais recentes. */
export async function listarPorUnidade(condominioId, unidadeId, limite = 24) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS_FATURA} FROM faturas
      WHERE condominio_id = $1 AND unidade_id = $2
      ORDER BY competencia DESC
      LIMIT $3`,
    [condominioId, unidadeId, limite],
  );
  return rows;
}

/**
 * Todos os itens de fatura de uma unidade, com contexto da fatura.
 * Usado na visão de gastos do morador.
 */
export async function listarItensUnidade(condominioId, unidadeId, ano) {
  const params = [condominioId, unidadeId];
  const filtroAno = ano ? ` AND EXTRACT(YEAR FROM f.competencia) = $3` : '';
  if (ano) params.push(Number(ano));

  const { rows } = await consultar(
    `SELECT fi.id, fi.tipo, fi.descricao, fi.valor_centavos AS "valorCentavos",
            f.id AS "faturaId", f.competencia, f.status AS "faturaStatus", f.vencimento
       FROM fatura_itens fi
       JOIN faturas f ON f.id = fi.fatura_id
      WHERE f.condominio_id = $1 AND f.unidade_id = $2
        AND f.status <> 'CANCELADA'${filtroAno}
      ORDER BY f.competencia DESC, fi.tipo, fi.descricao`,
    params,
  );
  return rows;
}

/** Fatura específica — só devolve se pertencer ao condomínio. */
export async function porId(condominioId, id) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS_FATURA} FROM faturas WHERE condominio_id = $1 AND id = $2`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

/** Lista de faturas do condomínio para o síndico, com filtros opcionais. */
export async function listarPorCondominio(condominioId, { competencia, status, unidadeId } = {}) {
  const condicoes = ['condominio_id = $1'];
  const params = [condominioId];

  if (competencia) {
    params.push(competencia);
    condicoes.push(`competencia = $${params.length}`);
  }
  if (status) {
    params.push(status);
    condicoes.push(`status = $${params.length}`);
  }
  if (unidadeId) {
    params.push(unidadeId);
    condicoes.push(`unidade_id = $${params.length}`);
  }

  const { rows } = await consultar(
    `SELECT ${CAMPOS_FATURA} FROM faturas
      WHERE ${condicoes.join(' AND ')}
      ORDER BY competencia DESC, criado_em`,
    params,
  );
  return rows;
}

/** Marca fatura como PAGA. Usa transação para ser atômica com o registro de cobrança. */
export async function marcarPaga(cliente, faturaId, dados) {
  const { rows } = await cliente.query(
    `UPDATE faturas
        SET status = 'PAGA', pago_em = $2, valor_pago_centavos = $3, forma_baixa = $4,
            atualizado_em = now()
      WHERE id = $1 AND status IN ('ABERTA', 'EM_ATRASO')
      RETURNING ${CAMPOS_FATURA}`,
    [faturaId, dados.pagoEm, dados.valorPagoCentavos, dados.formaBaixa],
  );
  return rows[0] ?? null;
}

/** Job noturno: marca como EM_ATRASO faturas abertas com vencimento no passado. */
export async function marcarEmAtraso() {
  const { rows } = await consultar(
    `UPDATE faturas SET status = 'EM_ATRASO', atualizado_em = now()
      WHERE status = 'ABERTA' AND vencimento < CURRENT_DATE
      RETURNING id, condominio_id AS "condominioId", responsavel_usuario_id AS "responsavelUsuarioId",
                competencia, valor_centavos AS "valorCentavos"`,
  );
  return rows;
}

/** KPIs para o painel do síndico. */
export async function kpisPorCompetencia(condominioId, competencia) {
  const { rows } = await consultar(
    `SELECT
       COUNT(*) FILTER (WHERE status <> 'CANCELADA') AS total,
       COALESCE(SUM(valor_centavos) FILTER (WHERE status <> 'CANCELADA'), 0) AS "totalCentavos",
       COUNT(*) FILTER (WHERE status = 'PAGA') AS pagas,
       COALESCE(SUM(valor_centavos) FILTER (WHERE status = 'PAGA'), 0) AS "pagasCentavos",
       COUNT(*) FILTER (WHERE status IN ('ABERTA', 'EM_ATRASO')) AS abertas,
       COUNT(*) FILTER (WHERE status = 'EM_ATRASO') AS "emAtraso"
     FROM faturas
     WHERE condominio_id = $1 AND competencia = $2`,
    [condominioId, competencia],
  );
  return rows[0];
}
