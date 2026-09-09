import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

/**
 * Devolve a mensalidade do plano ativo do condomínio, em centavos.
 *
 * Usado pelo fechamento para calcular a taxa da plataforma que é rateada
 * entre as unidades. Tolerante a falha: um plano indisponível não deve
 * impedir o fechamento do mês — retorna 0 com aviso.
 */
export async function buscarTaxaPlataforma(condominioId, authorization) {
  const r = await buscar(
    `${SERVICOS.plan}/api/assinaturas/condominio/${encodeURIComponent(condominioId)}`,
    authorization,
  );

  if (!r.ok) {
    console.warn(`[financeiro] plan-service indisponível para ${condominioId}: ${r.erro}`);
    return { ok: false, mensalidadeCentavos: 0 };
  }

  const mensalidade = r.dados?.mensalidade;
  if (!mensalidade) return { ok: true, mensalidadeCentavos: 0 };

  // O plan-service devolve mensalidade como BigDecimal (string ou number).
  const centavos = Math.round(Number(mensalidade) * 100);
  return { ok: true, mensalidadeCentavos: Number.isFinite(centavos) ? centavos : 0 };
}
