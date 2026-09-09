import * as notificacaoService from '../services/notificacaoService.js';

export async function listar(req, res) {
  const notificacoes = await notificacaoService.listar(req.claims.id);
  const naoLidas = await notificacaoService.naoLidas(req.claims.id);
  res.json({ sucesso: true, naoLidas, total: notificacoes.length, notificacoes });
}

export async function marcarLida(req, res) {
  const ok = await notificacaoService.marcarLida(req.params.id, req.claims.id);
  if (!ok) return res.status(404).json({ sucesso: false, mensagem: 'Notificação não encontrada.' });
  res.json({ sucesso: true });
}

export async function marcarTodasLidas(req, res) {
  const total = await notificacaoService.marcarTodasLidas(req.claims.id);
  res.json({ sucesso: true, marcadas: total });
}
