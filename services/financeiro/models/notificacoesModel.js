import { consultar } from '../config/database.js';

const CAMPOS = `id, usuario_id AS "usuarioId", condominio_id AS "condominioId",
  tipo, titulo, mensagem, lida, metadados, criado_em AS "criadoEm"`;

export async function criar(dados) {
  const { rows } = await consultar(
    `INSERT INTO notificacoes
       (usuario_id, condominio_id, tipo, titulo, mensagem, metadados)
     VALUES ($1, $2, $3, $4, $5, $6)
     RETURNING ${CAMPOS}`,
    [dados.usuarioId, dados.condominioId ?? null, dados.tipo,
      dados.titulo, dados.mensagem ?? null, dados.metadados ? JSON.stringify(dados.metadados) : null],
  );
  return rows[0];
}

export async function listarPorUsuario(usuarioId, limite = 30) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM notificacoes
      WHERE usuario_id = $1
      ORDER BY criado_em DESC
      LIMIT $2`,
    [usuarioId, limite],
  );
  return rows;
}

export async function contarNaoLidas(usuarioId) {
  const { rows } = await consultar(
    `SELECT COUNT(*)::int AS total FROM notificacoes WHERE usuario_id = $1 AND NOT lida`,
    [usuarioId],
  );
  return rows[0]?.total ?? 0;
}

export async function marcarLida(id, usuarioId) {
  const { rows } = await consultar(
    `UPDATE notificacoes SET lida = TRUE WHERE id = $1 AND usuario_id = $2 RETURNING id`,
    [id, usuarioId],
  );
  return rows.length > 0;
}

export async function marcarTodasLidas(usuarioId) {
  const { rowCount } = await consultar(
    `UPDATE notificacoes SET lida = TRUE WHERE usuario_id = $1 AND NOT lida`,
    [usuarioId],
  );
  return rowCount;
}
