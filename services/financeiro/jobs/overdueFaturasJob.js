import cron from 'node-cron';
import * as faturasModel from '../models/faturasModel.js';
import * as notificacaoService from '../services/notificacaoService.js';
import * as emailService from '../services/emailFinanceiroService.js';
import { formatarBRL } from '../utils/dinheiro.js';

function competenciaLabel(competenciaDate) {
  const [ano, mes] = String(competenciaDate).split('-');
  const meses = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun',
    'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  return `${meses[Number(mes) - 1]}/${ano}`;
}

async function marcarFaturasVencidas() {
  const atualizadas = await faturasModel.marcarEmAtraso();
  if (!atualizadas.length) return;

  console.log(`[overdue-job] ${atualizadas.length} fatura(s) marcadas como EM_ATRASO`);

  for (const f of atualizadas) {
    const label = competenciaLabel(String(f.competencia).slice(0, 7));

    await notificacaoService.criar(
      f.responsavelUsuarioId, f.condominioId,
      'FATURA_VENCIDA',
      'Fatura em atraso',
      `Sua fatura de ${label} venceu. Regularize o pagamento para evitar juros.`,
      { faturaId: f.id },
    );
  }
}

/** Registra o job — chamado uma vez no startup do servidor. */
export function iniciarJobOverdue() {
  // Roda às 01:00 todos os dias.
  cron.schedule('0 1 * * *', async () => {
    try {
      await marcarFaturasVencidas();
    } catch (err) {
      console.error('[overdue-job] falha:', err.message);
    }
  });
  console.log('[financeiro] job de inadimplência agendado (01:00 diário)');
}
