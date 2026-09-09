import { emTransacao, consultar } from '../config/database.js';
import * as cobrancasModel from '../models/cobrancasModel.js';
import * as faturasModel from '../models/faturasModel.js';
import * as notificacaoService from './notificacaoService.js';
import * as emailService from './emailFinanceiroService.js';
import { formatarBRL } from '../utils/dinheiro.js';

function competenciaLabel(competenciaDate) {
  const [ano, mes] = competenciaDate.split('-');
  const meses = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun',
    'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  return `${meses[Number(mes) - 1]}/${ano}`;
}

/**
 * Processa um evento do Asaas de forma idempotente.
 *
 * Gravar o evento e dar baixa na fatura são atômicos: se o processo cair
 * entre os dois, o banco fará rollback e a segunda entrega do webhook vai
 * processar normalmente. Sem isso, metade das entregas de retry criariam
 * pagamentos duplicados.
 */
export async function processar(payload) {
  const eventoId = payload.id;
  if (!eventoId) return { ignorado: true, motivo: 'sem id' };

  const asaasPagamentoId = payload.payment?.id;
  const tipo = payload.event;

  if (!['PAYMENT_RECEIVED', 'PAYMENT_CONFIRMED', 'PAYMENT_OVERDUE',
    'PAYMENT_DELETED', 'PAYMENT_REFUND_REVERSED'].includes(tipo)) {
    return { ignorado: true, motivo: `evento ${tipo} não tratado` };
  }

  try {
    await emTransacao(async (cliente) => {
      // Idempotência: falha se o evento já foi registrado (UNIQUE no evento_id).
      await cliente.query(
        `INSERT INTO webhook_eventos (evento_id, tipo, asaas_pagamento_id, payload)
         VALUES ($1, $2, $3, $4)`,
        [eventoId, tipo, asaasPagamentoId ?? null, JSON.stringify(payload)],
      );

      if (!asaasPagamentoId) return;

      const cobranca = await cobrancasModel.porAsaasId(asaasPagamentoId);
      if (!cobranca) return; // webhook de cobrança fora deste sistema

      if (tipo === 'PAYMENT_RECEIVED' || tipo === 'PAYMENT_CONFIRMED') {
        await cobrancasModel.atualizar(cliente, cobranca.id, { status: 'RECEBIDA' });

        const fatura = await faturasModel.marcarPaga(cliente, cobranca.faturaId, {
          pagoEm: new Date().toISOString(),
          valorPagoCentavos: payload.payment?.value
            ? Math.round(Number(payload.payment.value) * 100)
            : cobranca.valorCentavos,
          formaBaixa: 'GATEWAY',
        });

        if (fatura) {
          const label = competenciaLabel(String(fatura.competencia).slice(0, 7));
          await notificacaoService.criar(
            fatura.responsavelUsuarioId, fatura.condominioId,
            'PAGAMENTO_CONFIRMADO',
            'Pagamento confirmado',
            `Seu pagamento da fatura de ${label} foi confirmado. ${formatarBRL(fatura.valorPagoCentavos ?? fatura.valorCentavos)} recebido.`,
            { faturaId: fatura.id },
          );
        }
      } else if (tipo === 'PAYMENT_OVERDUE') {
        await cobrancasModel.atualizar(cliente, cobranca.id, { status: 'VENCIDA' });
        const { rows } = await cliente.query(
          `UPDATE faturas SET status = 'EM_ATRASO', atualizado_em = now()
            WHERE id = $1 AND status = 'ABERTA'
            RETURNING id, competencia, condominio_id AS "condominioId",
                      responsavel_usuario_id AS "responsavelUsuarioId",
                      valor_centavos AS "valorCentavos"`,
          [cobranca.faturaId],
        );
        const fatura = rows[0];
        if (fatura) {
          const label = competenciaLabel(String(fatura.competencia).slice(0, 7));
          await notificacaoService.criar(
            fatura.responsavelUsuarioId, fatura.condominioId,
            'FATURA_VENCIDA',
            'Fatura em atraso',
            `Sua fatura de ${label} está em atraso. Regularize o pagamento.`,
            { faturaId: fatura.id },
          );
        }
      } else if (tipo === 'PAYMENT_DELETED' || tipo === 'PAYMENT_REFUND_REVERSED') {
        await cobrancasModel.atualizar(cliente, cobranca.id, { status: 'CANCELADA' });
        await cliente.query(
          `UPDATE faturas SET status = 'CANCELADA', atualizado_em = now()
            WHERE id = $1 AND status IN ('ABERTA', 'EM_ATRASO')`,
          [cobranca.faturaId],
        );
      }
    });

    return { processado: true };
  } catch (err) {
    // 23505 = unique_violation do evento_id: entrega duplicada, já processada.
    if (err.code === '23505') return { ignorado: true, motivo: 'duplicado' };
    throw err;
  }
}
