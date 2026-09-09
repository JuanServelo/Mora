import * as webhookService from '../services/webhookService.js';

const TOKEN = process.env.ASAAS_WEBHOOK_TOKEN;

export async function processar(req, res) {
  // Verificação de assinatura via token no header (quando configurado).
  if (TOKEN) {
    const assinatura = req.headers['asaas-webhook-token'] ?? req.headers['access_token'];
    if (assinatura !== TOKEN) {
      return res.status(401).json({ sucesso: false, mensagem: 'Assinatura inválida.' });
    }
  }

  const payload = req.body;
  if (!payload?.event) {
    return res.status(400).json({ sucesso: false, mensagem: 'Payload inválido.' });
  }

  try {
    const resultado = await webhookService.processar(payload);
    // Asaas considera qualquer 2xx como entrega bem-sucedida.
    res.json({ sucesso: true, ...resultado });
  } catch (err) {
    console.error('[financeiro] webhook erro:', err.message);
    // 500 faz o Asaas reenviar — correto para falhas transitórias.
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar evento.' });
  }
}
