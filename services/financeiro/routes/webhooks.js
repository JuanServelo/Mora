import express from 'express';
import * as ctrl from '../controllers/webhookController.js';

const router = express.Router();

// O parse do corpo cru já acontece no server.js antes do JSON global.
router.post('/webhooks', ctrl.processar);

export default router;
