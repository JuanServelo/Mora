import { consultar } from '../config/database.js';

const CAMPOS = `id, condominio_id AS "condominioId", tipo, descricao,
                competencia, valor_total_centavos AS "valorTotalCentavos",
                modo_rateio AS "modoRateio", vencimento, status,
                comprovante_url AS "comprovanteUrl",
                criado_por_id AS "criadoPorId",
                criado_em AS "criadoEm", atualizado_em AS "atualizadoEm"`;

export async function listar(condominioId, competencia) {
  const params = [condominioId];
  const filtroComp = competencia ? `AND competencia = $2` : '';
  if (competencia) params.push(competencia);

  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM contas_consumo
      WHERE condominio_id = $1 ${filtroComp} AND status <> 'CANCELADA'
      ORDER BY competencia DESC, criado_em DESC`,
    params,
  );
  return rows;
}

export async function porId(condominioId, id) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM contas_consumo WHERE condominio_id = $1 AND id = $2`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

export async function criar(condominioId, d) {
  const { rows } = await consultar(
    `INSERT INTO contas_consumo
       (condominio_id, tipo, descricao, competencia, valor_total_centavos,
        modo_rateio, vencimento, criado_por_id)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     RETURNING ${CAMPOS}`,
    [condominioId, d.tipo, d.descricao ?? null, d.competencia,
      d.valorTotalCentavos, d.modoRateio, d.vencimento, d.criadoPorId ?? null],
  );
  return rows[0];
}

export async function atualizar(condominioId, id, d) {
  const { rows } = await consultar(
    `UPDATE contas_consumo
        SET tipo = $3, descricao = $4, valor_total_centavos = $5,
            modo_rateio = $6, vencimento = $7, comprovante_url = $8,
            atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2 AND status = 'PENDENTE'
      RETURNING ${CAMPOS}`,
    [condominioId, id, d.tipo, d.descricao ?? null, d.valorTotalCentavos,
      d.modoRateio, d.vencimento, d.comprovanteUrl ?? null],
  );
  return rows[0] ?? null;
}

export async function cancelar(condominioId, id) {
  const { rows } = await consultar(
    `UPDATE contas_consumo SET status = 'CANCELADA', atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2 AND status <> 'CANCELADA'
      RETURNING ${CAMPOS}`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

/**
 * Contas a incluir no fechamento — PENDENTE e RATEADA.
 *
 * RATEADA é incluída porque o ratear antecipado pode ter rodado com 0 faturas
 * existentes (sem inserir nada) e mesmo assim mudado o status. O fechamento
 * sempre recalcula os itens do zero, então incluir RATEADA é idempotente.
 */
export async function listarParaFaturamento(condominioId, competencia) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM contas_consumo
      WHERE condominio_id = $1 AND competencia = $2 AND status IN ('PENDENTE', 'RATEADA')`,
    [condominioId, competencia],
  );
  return rows;
}

export async function marcarRateada(id) {
  await consultar(
    `UPDATE contas_consumo SET status = 'RATEADA', atualizado_em = now() WHERE id = $1`,
    [id],
  );
}
