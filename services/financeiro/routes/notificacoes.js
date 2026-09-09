import express from 'express';
import { autenticar } from '../middleware/auth.js';
import * as ctrl from '../controllers/notificacoesController.js';

const router = express.Router();

// Qualquer usuário autenticado pode ver e marcar suas próprias notificações.
router.use(autenticar);

router.get('/notificacoes', ctrl.listar);
router.patch('/notificacoes/:id/lida', ctrl.marcarLida);
router.patch('/notificacoes/marcar-todas-lidas', ctrl.marcarTodasLidas);

export default router;
