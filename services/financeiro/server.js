// Primeiro import de todos, e nao por estilo: em ESM os modulos importados sao
// avaliados antes do corpo deste arquivo. Um `dotenv.config()` la embaixo
// rodaria depois de config/database.js ja ter lido process.env e montado o pool
// sem senha. Em Docker isso passa batido, porque o env vem do container.
import 'dotenv/config';

import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import contextoRoutes from './routes/contexto.js';
import cadastrosRoutes from './routes/cadastros.js';
import gatewayRoutes from './routes/gateway.js';
import contasConsumoRoutes from './routes/contasConsumo.js';
import faturasRoutes from './routes/faturas.js';
import webhooksRoutes from './routes/webhooks.js';
import notificacoesRoutes from './routes/notificacoes.js';
import { PORT, SERVICOS, ehProducao } from './config/servicos.js';
import { registrarNoConsul } from './config/consul.js';
import { verificarConexao } from './config/database.js';
import { gatewayConfigurado } from './config/asaas.js';
import { iniciarJobOverdue } from './jobs/overdueFaturasJob.js';

// O segredo é compartilhado com o auth-api para validar os tokens que ele emite.
if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

if (!process.env.POSTGRES_PASSWORD) {
  console.error('ERRO: POSTGRES_PASSWORD não definido no .env');
  process.exit(1);
}

const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));

// O webhook do gateway precisa do corpo cru para conferir a assinatura, então
// entra antes do parser de JSON e guarda os bytes originais.
app.use('/api/financeiro/webhooks', express.json({
  limit: '256kb',
  verify: (req, _res, buf) => { req.corpoCru = buf; },
}));

app.use(express.json({ limit: '64kb' }));

app.use('/api/financeiro', contextoRoutes);
app.use('/api/financeiro', cadastrosRoutes);
app.use('/api/financeiro', gatewayRoutes);
app.use('/api/financeiro', contasConsumoRoutes);
app.use('/api/financeiro', faturasRoutes);
app.use('/api/financeiro', webhooksRoutes);
app.use('/api/financeiro', notificacoesRoutes);

app.get('/health', async (_req, res) => {
  const banco = await verificarConexao().catch(() => false);
  res.status(banco ? 200 : 503).json({
    status: banco ? 'ok' : 'degradado',
    servico: 'financeiro',
    banco: banco ? 'ok' : 'indisponivel',
    gateway: gatewayConfigurado() ? 'configurado' : 'ausente',
  });
});

app.use((err, _req, res, _next) => {
  console.error('[financeiro] erro não tratado:', err);
  // Mensagem genérica de propósito: erro de banco costuma vazar nome de tabela
  // e trecho de SQL para quem chamou.
  res.status(500).json({ sucesso: false, mensagem: 'Erro interno.' });
});

const server = app.listen(PORT, async () => {
  console.log(`financeiro rodando em http://localhost:${PORT}`);
  console.log(`  auth-api: ${SERVICOS.auth}`);
  console.log(`  banco:    ${process.env.POSTGRES_DB || 'mora_financeiro'}`);
  console.log(`  gateway:  ${gatewayConfigurado() ? 'configurado' : 'ASAAS_API_KEY ausente'}`);
  if (!ehProducao()) console.log('  ambiente: desenvolvimento');
  iniciarJobOverdue();
  await registrarNoConsul();
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Porta ${PORT} em uso.`);
    process.exit(1);
  }
  throw err;
});
