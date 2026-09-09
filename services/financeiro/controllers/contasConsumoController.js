import * as service from '../services/contasConsumoService.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

export async function listar(req, res) {
  const itens = await service.listar(req.escopo.condominioId, req.query.competencia);
  res.json({ sucesso: true, total: itens.length, itens });
}

export async function criar(req, res) {
  const r = await service.criar(req.escopo.condominioId, req.body ?? {}, req.escopo.usuarioId);
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, conta: r });
}

export async function atualizar(req, res) {
  const r = await service.atualizar(req.escopo.condominioId, req.params.id, req.body ?? {});
  if (seErro(r, res)) return;
  res.json({ sucesso: true, conta: r });
}

export async function cancelar(req, res) {
  const r = await service.cancelar(req.escopo.condominioId, req.params.id);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, conta: r });
}

export async function ratear(req, res) {
  const r = await service.ratear(req.escopo.condominioId, req.params.id);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
}
