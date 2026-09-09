import { consultar } from '../config/database.js';

const CAMPOS = `condominio_id AS "condominioId", modo,
                dia_fechamento AS "diaFechamento", dia_vencimento AS "diaVencimento",
                dias_recurso_multa AS "diasRecursoMulta",
                incluir_taxa_plataforma AS "incluirTaxaPlataforma"`;

/**
 * Devolve as regras do condomínio, criando-as com o padrão na primeira leitura.
 *
 * Assim o síndico nunca encontra a tela vazia sem saber o que preencher, e o
 * fechamento sempre acha uma configuração para ler.
 */
export async function obterOuCriar(condominioId) {
  const { rows } = await consultar(
    `INSERT INTO regras_taxa (condominio_id) VALUES ($1)
     ON CONFLICT (condominio_id) DO UPDATE SET condominio_id = EXCLUDED.condominio_id
     RETURNING ${CAMPOS}`,
    [condominioId],
  );
  return rows[0];
}

export async function atualizar(condominioId, r) {
  const { rows } = await consultar(
    `UPDATE regras_taxa
        SET modo = $2, dia_fechamento = $3, dia_vencimento = $4,
            dias_recurso_multa = $5,
            incluir_taxa_plataforma = COALESCE($6, incluir_taxa_plataforma),
            atualizado_em = now()
      WHERE condominio_id = $1
      RETURNING ${CAMPOS}`,
    [condominioId, r.modo, r.diaFechamento, r.diaVencimento, r.diasRecursoMulta,
      r.incluirTaxaPlataforma ?? null],
  );
  return rows[0] ?? null;
}
