import { consultar } from '../config/database.js';

const CAMPOS = `id, fatura_id AS "faturaId", condominio_id AS "condominioId",
  asaas_id AS "asaasId", billing_type AS "billingType", status,
  valor_centavos AS "valorCentavos", vencimento,
  url_boleto AS "urlBoleto", pix_payload AS "pixPayload", pix_qrcode AS "pixQrcode",
  criado_em AS "criadoEm"`;

export async function criar(dados) {
  const { rows } = await consultar(
    `INSERT INTO cobrancas
       (fatura_id, condominio_id, asaas_id, billing_type, status,
        valor_centavos, vencimento, url_boleto, pix_payload, pix_qrcode)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
     RETURNING ${CAMPOS}`,
    [dados.faturaId, dados.condominioId, dados.asaasId, dados.billingType,
      dados.status ?? 'PENDENTE', dados.valorCentavos, dados.vencimento,
      dados.urlBoleto ?? null, dados.pixPayload ?? null, dados.pixQrcode ?? null],
  );
  return rows[0];
}

export async function porFatura(faturaId) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM cobrancas WHERE fatura_id = $1 ORDER BY criado_em DESC LIMIT 1`,
    [faturaId],
  );
  return rows[0] ?? null;
}

export async function porFaturaEForma(faturaId, billingType) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM cobrancas WHERE fatura_id = $1 AND billing_type = $2 ORDER BY criado_em DESC LIMIT 1`,
    [faturaId, billingType],
  );
  return rows[0] ?? null;
}

export async function porAsaasId(asaasId) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM cobrancas WHERE asaas_id = $1`,
    [asaasId],
  );
  return rows[0] ?? null;
}

export async function atualizar(cliente, id, dados) {
  const { rows } = await cliente.query(
    `UPDATE cobrancas
        SET status = COALESCE($2, status),
            url_boleto = COALESCE($3, url_boleto),
            pix_payload = COALESCE($4, pix_payload),
            pix_qrcode = COALESCE($5, pix_qrcode),
            atualizado_em = now()
      WHERE id = $1
      RETURNING ${CAMPOS}`,
    [id, dados.status ?? null, dados.urlBoleto ?? null,
      dados.pixPayload ?? null, dados.pixQrcode ?? null],
  );
  return rows[0] ?? null;
}
