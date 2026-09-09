import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO, PERFIS_UNIDADE } from '../middleware/auth.js';
import { resolverEscopo, resolverUnidade, exigirEscrita } from '../middleware/escopo.js';
import * as ctrl from '../controllers/faturasController.js';

const router = express.Router();

// ── Morador: faturas da própria unidade ──────────────────────────────────────

router.get(
  '/faturas',
  autenticar,
  exigirPerfis(...PERFIS_UNIDADE),
  resolverEscopo,
  resolverUnidade,
  ctrl.minhasFaturas,
);

router.get(
  '/faturas/:id',
  autenticar,
  exigirPerfis(...PERFIS_UNIDADE),
  resolverEscopo,
  resolverUnidade,
  ctrl.detalhesFatura,
);

router.get(
  '/gastos',
  autenticar,
  exigirPerfis(...PERFIS_UNIDADE),
  resolverEscopo,
  resolverUnidade,
  ctrl.meuGastos,
);

router.post(
  '/faturas/:id/pagar',
  autenticar,
  exigirPerfis(...PERFIS_UNIDADE),
  resolverEscopo,
  resolverUnidade,
  ctrl.gerarCobranca,
);

// ── Síndico: gestão de faturas do condomínio ─────────────────────────────────

router.get(
  '/admin/faturas',
  autenticar,
  exigirPerfis(...PERFIS_GESTAO),
  resolverEscopo,
  ctrl.listarFaturas,
);

router.get(
  '/admin/faturas/kpis',
  autenticar,
  exigirPerfis(...PERFIS_GESTAO),
  resolverEscopo,
  ctrl.kpis,
);

router.get(
  '/admin/fechamento/preview',
  autenticar,
  exigirPerfis(...PERFIS_GESTAO),
  resolverEscopo,
  ctrl.preview,
);

router.post(
  '/fechar-competencia',
  autenticar,
  exigirPerfis('ADMIN_SINDICO'),
  resolverEscopo,
  exigirEscrita,
  ctrl.fecharCompetencia,
);

router.patch(
  '/admin/faturas/:id/baixa-manual',
  autenticar,
  exigirPerfis('ADMIN_SINDICO'),
  resolverEscopo,
  exigirEscrita,
  ctrl.baixaManual,
);

// ── Admin Geral: taxa da plataforma ──────────────────────────────────────────

router.get(
  '/admin/taxa-plataforma',
  autenticar,
  exigirPerfis('ADMIN_GERAL'),
  resolverEscopo,
  ctrl.taxaPlataformaAdmin,
);

export default router;
