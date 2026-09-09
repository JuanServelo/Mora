import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO } from '../middleware/auth.js';
import { resolverEscopo, exigirEscrita } from '../middleware/escopo.js';
import * as taxas from '../services/taxaService.js';
import * as fracoes from '../services/fracaoService.js';

const router = express.Router();

const adm = [autenticar, exigirPerfis(...PERFIS_GESTAO), resolverEscopo];
const admW = [...adm, exigirEscrita];

/** Responde o erro que o service devolveu, quando houver. */
function seErro(resultado, res) {
  if (!resultado?.erro) return false;
  res.status(resultado.status ?? 400).json({ sucesso: false, mensagem: resultado.mensagem });
  return true;
}

// ── Regras de fechamento ────────────────────────────────────────────────────

router.get('/config', adm, async (req, res) => {
  const r = await taxas.obterRegras(req.escopo.condominioId);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, config: r });
});

router.put('/config', admW, async (req, res) => {
  const r = await taxas.atualizarRegras(req.escopo.condominioId, req.body ?? {});
  if (seErro(r, res)) return;
  res.json({ sucesso: true, config: r });
});

// ── Tipos de taxa ───────────────────────────────────────────────────────────

router.get('/tipos-taxa', adm, async (req, res) => {
  const itens = await taxas.listarTipos(req.escopo.condominioId, req.query.todos === 'true');
  res.json({ sucesso: true, total: itens.length, itens });
});

router.post('/tipos-taxa', admW, async (req, res) => {
  const r = await taxas.criarTipo(req.escopo.condominioId, req.body ?? {});
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, tipo: r });
});

router.put('/tipos-taxa/:id', admW, async (req, res) => {
  const r = await taxas.atualizarTipo(req.escopo.condominioId, req.params.id, req.body ?? {});
  if (seErro(r, res)) return;
  res.json({ sucesso: true, tipo: r });
});

router.delete('/tipos-taxa/:id', admW, async (req, res) => {
  const r = await taxas.desativarTipo(req.escopo.condominioId, req.params.id);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, tipo: r });
});

// ── Fração ideal ────────────────────────────────────────────────────────────

router.get('/fracoes', adm, async (req, res) => {
  const r = await fracoes.listar(req.escopo.condominioId, req.authorization);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
});

router.put('/fracoes/:unidadeId', admW, async (req, res) => {
  const r = await fracoes.definir(
    req.escopo.condominioId, req.params.unidadeId, req.body?.milesimos,
  );
  if (seErro(r, res)) return;
  res.json({ sucesso: true, fracao: r });
});

router.delete('/fracoes/:unidadeId', admW, async (req, res) => {
  const r = await fracoes.remover(req.escopo.condominioId, req.params.unidadeId);
  if (seErro(r, res)) return;
  res.json({ sucesso: true });
});

router.get('/fracoes/propor-por-area', adm, async (req, res) => {
  const r = await fracoes.proporPorArea(req.escopo.condominioId, req.authorization);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
});

router.post('/fracoes/aplicar-lote', admW, async (req, res) => {
  const r = await fracoes.aplicarLote(req.escopo.condominioId, req.body?.fracoes);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
});

export default router;
