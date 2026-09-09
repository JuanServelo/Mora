import * as notificacoesModel from '../models/notificacoesModel.js';

export async function criar(usuarioId, condominioId, tipo, titulo, mensagem, metadados) {
  if (!usuarioId) return null;
  try {
    return await notificacoesModel.criar({ usuarioId, condominioId, tipo, titulo, mensagem, metadados });
  } catch (err) {
    console.warn('[financeiro] falha ao criar notificação:', err.message);
    return null;
  }
}

export async function listar(usuarioId) {
  return notificacoesModel.listarPorUsuario(usuarioId);
}

export async function naoLidas(usuarioId) {
  return notificacoesModel.contarNaoLidas(usuarioId);
}

export async function marcarLida(id, usuarioId) {
  return notificacoesModel.marcarLida(id, usuarioId);
}

export async function marcarTodasLidas(usuarioId) {
  return notificacoesModel.marcarTodasLidas(usuarioId);
}
