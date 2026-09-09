import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import helmet from 'helmet';
import session from 'express-session';
import passport from 'passport';
import path from 'path';
import { fileURLToPath } from 'url';
import sequelize from './config/database.js';
import setupPassport from './config/passport.js';
import './models/index.js';
import User from './models/User.js';
import Condominio from './models/Condominio.js';
import Reclamacao from './models/Reclamacao.js';
import authRoutes from './routes/auth.js';
import usersRoutes from './routes/users.js';
import invitesRoutes from './routes/invites.js';
import userManagementRoutes from './routes/user-management.js';
import reclamacoesRoutes from './routes/reclamacoes.js';
import condominiosRoutes from './routes/condominios.js';
import perfisRoutes from './routes/perfis.js';
import portariaRoutes from './routes/portaria.js';
import estatisticasRoutes from './routes/estatisticas.js';
import { garantirColunasNovas, migrarUsuariosLegados } from './migrations/migrate-rf03.js';
import { garantirColunasRf07 } from './migrations/migrate-rf07.js';
import { garantirTabelaCondominios } from './migrations/migrate-condominios.js';
import { garantirTabelaPortaria } from './migrations/migrate-portaria.js';
import { garantirColunasOauthCode } from './migrations/migrate-oauth-code.js';
import { migrarPerfisV2 } from './migrations/migrate-perfis-v2.js';
import { garantirCondominioIdReclamacoes } from './migrations/migrate-condominio-id.js';
import { garantirColunasInvites } from './migrations/migrate-invites.js';
import { migrarPerfilTerceiro } from './migrations/migrate-terceiro.js';
import { PERFIS, STATUS_USUARIO } from './constants/perfis.js';
import { ehProducao } from './config/regras.js';

dotenv.config();

const __dirname = path.dirname(fileURLToPath(import.meta.url));

Reclamacao.belongsTo(User, { foreignKey: 'userId', as: 'usuario' });
User.hasMany(Reclamacao, { foreignKey: 'userId', as: 'reclamacoes' });

if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

// A sessão assina o `state` do OAuth. Reutilizar o JWT_SECRET faria as duas
// chaves caírem juntas se uma vazasse.
if (!process.env.SESSION_SECRET) {
  if (ehProducao()) {
    console.error('ERRO: SESSION_SECRET não definido no .env');
    process.exit(1);
  }
  console.warn('SESSION_SECRET não definido — usando fallback (apenas desenvolvimento)');
}

setupPassport();

const app = express();
const PORT = process.env.PORT || 3001;

app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '100kb' }));

// A sessão existe só para guardar o `state` do OAuth entre o redirect e o
// callback — daí o cookie curto. A autenticação em si é via JWT.
app.use(session({
  name: 'mora.oauth',
  secret: process.env.SESSION_SECRET || process.env.JWT_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: ehProducao(),
    maxAge: 5 * 60 * 1000,
  },
}));
app.use(passport.initialize());

const uploadDir = process.env.STORAGE_PATH || path.join(__dirname, 'uploads', 'avatars');
app.use('/uploads/avatars', express.static(uploadDir));

app.use('/api/auth', authRoutes);
app.use('/api/invites', invitesRoutes);
app.use('/api/user-management', userManagementRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/reclamacoes', reclamacoesRoutes);
app.use('/api/condominios', condominiosRoutes);
app.use('/api/perfis', perfisRoutes);
app.use('/api/portaria', portariaRoutes);
app.use('/api/estatisticas', estatisticasRoutes);

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Auth API funcionando' });
});

/**
 * Cria a conta administrativa inicial a partir do .env. A senha nunca fica no
 * código: sem ADMIN_SEED_PASSWORD definido, o seed é pulado — em produção isso
 * é obrigatório, e em desenvolvimento apenas avisa.
 */
const seedAdminUser = async () => {
  const existing = await User.findOne({ where: { role: 'admin' } });
  if (existing) return;

  const adminEmail = process.env.ADMIN_SEED_EMAIL;
  const adminSenha = process.env.ADMIN_SEED_PASSWORD;

  if (!adminEmail || !adminSenha) {
    console.warn(
      'ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD não definidos — seed do admin pulado. '
      + 'Defina-os no .env para criar a conta administrativa inicial.',
    );
    return;
  }

  await User.create({
    nome: process.env.ADMIN_SEED_NOME || 'Admin',
    email: adminEmail,
    senha: adminSenha,
    role: 'admin',
    perfil: PERFIS.ADMIN_GERAL,
    status: STATUS_USUARIO.ACTIVE,
    activatedAt: new Date(),
  });
  console.log(`Usuário admin criado: ${adminEmail}`);
};

const startServer = async () => {
  try {
    await sequelize.authenticate();
    console.log('PostgreSQL conectado');
    await garantirColunasNovas();
    await garantirColunasRf07();
    await garantirTabelaCondominios();
    await garantirColunasOauthCode();
    await migrarUsuariosLegados();
    // Depois das legadas: converte os 11 perfis antigos para os 6 atuais.
    await migrarPerfisV2();
    await garantirCondominioIdReclamacoes();
    await garantirColunasInvites();
    await migrarPerfilTerceiro();
    console.log('Tabelas sincronizadas e migrações RF03/RF07/Condomínios aplicadas');
    await seedAdminUser();
  } catch (err) {
    console.error('Erro ao sincronizar tabelas:', err.message);
  }

  // Migração isolada: tabela registros_acesso e colunas de portaria.
  // Bloco separado para garantir execução mesmo se outras migrações falharem.
  try {
    await garantirTabelaPortaria();
  } catch (err) {
    console.error('Erro ao migrar tabela portaria:', err.message);
  }

  const server = app.listen(PORT, () => {
    console.log(`Servidor rodando em http://localhost:${PORT}`);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.error(`Porta ${PORT} em uso.`);
      process.exit(1);
    }
    throw err;
  });
};

startServer();
