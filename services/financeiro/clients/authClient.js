import { SERVICOS } from '../config/servicos.js';
import { buscar } from '../utils/httpClient.js';

/**
 * Resolve a unidade do próprio usuário a partir do token dele.
 *
 * O JWT emitido pelo auth-api carrega `{ id, perfil, tokenVersion, email,
 * condominioId }` — e não `unidadeId`. Escopo de unidade, então, não sai do
 * token: precisa ser perguntado. `GET /api/auth/me` responde com o Bearer do
 * próprio interessado, sem credencial de serviço.
 *
 * Cache curto porque isto entra no caminho de toda listagem de fatura de
 * morador, e o dado muda raramente — mudança de unidade é evento de cadastro.
 */
const CACHE_MS = Number(process.env.CACHE_IDENTIDADE_MS || 60_000);
const cache = new Map();

export async function identidadeDoAtor(usuarioId, authorization) {
  const guardado = cache.get(usuarioId);
  if (guardado && guardado.ate > Date.now()) return guardado.valor;

  const r = await buscar(`${SERVICOS.auth}/api/auth/me`, authorization);
  // Sem resposta não há escopo: quem chama trata como negação, nunca como
  // "sem filtro". Filtro vazio numa listagem de faturas devolveria o
  // condomínio inteiro para um morador.
  if (!r.ok) return null;

  const u = r.dados?.usuario;
  if (!u) return null;

  const valor = {
    usuarioId: u.id,
    nome: u.nome ?? null,
    email: u.email ?? null,
    cpf: u.cpf ?? null,
    condominioId: u.condominioId ?? null,
    unidadeId: u.unidadeId ?? null,
    responsavelFinanceiro: Boolean(u.responsavelFinanceiro),
  };

  cache.set(usuarioId, { valor, ate: Date.now() + CACHE_MS });
  return valor;
}

/** Usado quando o vínculo do usuário muda e o cache ficaria velho. */
export function esquecerIdentidade(usuarioId) {
  cache.delete(usuarioId);
}

/**
 * Lista os moradores de uma unidade com seus dados públicos (inclui CPF e e-mail).
 *
 * Requer um token de ADMIN_GERAL ou ADMIN_SINDICO; o financeiro-service repassa
 * a credencial de quem disparou o fechamento.
 */
export async function listarResidentesPorUnidade(unidadeId, authorization) {
  const url = `${SERVICOS.auth}/api/user-management/units/${encodeURIComponent(unidadeId)}/residents`;
  return buscar(url, authorization);
}
