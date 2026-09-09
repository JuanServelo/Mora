import express from 'express';
import jwt from 'jsonwebtoken';
import passport from 'passport';
import rateLimit from 'express-rate-limit';
import multer from 'multer';
import sharp from 'sharp';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { Op } from 'sequelize';
import User from '../models/User.js';
import authMiddleware from '../middleware/auth.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';
import { enviarEmailReset, emailConfigurado } from '../utils/emailService.js';
import { googleOAuthConfigurado } from '../utils/oauthConfig.js';
import { validarSenha } from '../utils/passwordValidation.js';
import { redirectPorPerfil } from '../utils/redirectPorPerfil.js';
import { STATUS_USUARIO } from '../constants/perfis.js';
import { gerarTokenComHash, hashToken, expiraEm } from '../utils/tokens.js';
import { RESET_TOKEN_TTL_MS, OAUTH_CODE_TTL_MS, JWT_EXPIRES_IN } from '../config/regras.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const UPLOAD_DIR = process.env.STORAGE_PATH || path.join(__dirname, '..', 'uploads', 'avatars');

if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

const router = express.Router();

const limiter = (max, windowMs, mensagem) => rateLimit({
  windowMs,
  max,
  message: { sucesso: false, mensagem },
  standardHeaders: true,
  legacyHeaders: false,
});

// Limitadores separados por finalidade: estourar o login não pode impedir
// alguém de redefinir a senha ou concluir o OAuth do mesmo IP (NAT/escritório).
const authLimiter = limiter(10, 15 * 60 * 1000, 'Muitas tentativas de login. Tente novamente em 15 minutos.');
const forgotLimiter = limiter(5, 60 * 60 * 1000, 'Muitas solicitações de recuperação. Tente novamente em 1 hora.');
// Rotas protegidas por um token imprevisível: o limite serve só contra força bruta.
const tokenLimiter = limiter(20, 15 * 60 * 1000, 'Muitas tentativas. Tente novamente em 15 minutos.');

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
});

const getJwtSecret = () => process.env.JWT_SECRET;
const getFrontendUrl = () => process.env.FRONTEND_URL || 'http://localhost:5173';

function erroOAuthRedirect(err) {
  const msg = err?.message || '';
  if (msg.includes('Conta não encontrada')) return 'sem-convite';
  if (msg.includes('Cadastro pendente')) return 'cadastro_pendente';
  if (msg.includes('Conta desativada')) return 'conta_desativada';
  return 'oauth';
}

function loginComErroRedirect(codigo, extra = '') {
  if (codigo === 'sem-convite') {
    return `${getFrontendUrl()}/sem-convite?origem=google${extra}`;
  }
  return `${getFrontendUrl()}/login?erro=${codigo}${extra}`;
}

export const signToken = (userId, perfil, tokenVersion = 0, email = undefined, condominioId = undefined) =>
  jwt.sign(
    {
      id: userId,
      perfil,
      tokenVersion,
      ...(email != null && { email }),
      ...(condominioId != null && { condominioId }),
    },
    getJwtSecret(),
    { expiresIn: JWT_EXPIRES_IN },
  );

router.post('/register', authLimiter, (_req, res) => {
  res.status(403).json({
    sucesso: false,
    mensagem: 'Cadastro disponível apenas via código de convite. Acesse a página de ativação.',
  });
});

router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, senha } = req.body;

    if (!email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email e senha são obrigatórios' });
    }

    const usuario = await User.scope('withPassword').findOne({
      where: { email: email.toLowerCase().trim() },
    });

    if (!usuario || !usuario.senha) {
      return res.status(401).json({ sucesso: false, mensagem: 'E-mail ou senha inválidos.' });
    }

    if (usuario.status === STATUS_USUARIO.INACTIVE) {
      return res.status(401).json({ sucesso: false, mensagem: 'Esta conta foi desativada.' });
    }

    if (usuario.status === STATUS_USUARIO.PENDING_ACTIVATION) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Seu cadastro ainda não foi concluído. Acesse o link enviado para seu e-mail para criar sua senha.',
      });
    }

    if (usuario.semAcessoSistema) {
      return res.status(401).json({ sucesso: false, mensagem: 'Este perfil não possui acesso ao sistema. Procure a administração do condomínio.' });
    }

    const senhaValida = await usuario.compararSenha(senha);
    if (!senhaValida) {
      return res.status(401).json({ sucesso: false, mensagem: 'E-mail ou senha inválidos.' });
    }

    const perfil = usuario.getPerfilEfetivo();
    const token = signToken(usuario.id, perfil, usuario.tokenVersion || 0, usuario.email, usuario.condominioId);

    res.json({
      sucesso: true,
      mensagem: 'Login realizado com sucesso',
      token,
      usuario: usuarioPublico(usuario),
      redirectPath: redirectPorPerfil(perfil),
    });
  } catch (err) {
    console.error('Erro no login:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao fazer login' });
  }
});

router.get('/me', authMiddleware, async (req, res) => {
  try {
    res.json({
      sucesso: true,
      usuario: usuarioPublico(req.user),
    });
  } catch (err) {
    console.error('Erro ao buscar usuário:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao buscar usuário' });
  }
});

router.put('/me', authMiddleware, async (req, res) => {
  try {
    const { nome, email, telefone, senha, senhaAtual } = req.body;
    const usuario = await User.scope('withPassword').findByPk(req.userId);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    const erros = {};
    if (nome !== undefined && (!nome || !String(nome).trim())) {
      erros.nome = 'Este campo é obrigatório.';
    }
    if (email !== undefined && (!email || !String(email).trim())) {
      erros.email = 'Este campo é obrigatório.';
    }
    if (Object.keys(erros).length > 0) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Este campo é obrigatório.',
        erros,
      });
    }

    if (nome) usuario.nome = nome.trim();
    if (telefone !== undefined) usuario.telefone = telefone?.trim() || null;
    if (email) {
      const emailNorm = email.toLowerCase().trim();

      // Conta Google: o email é a identidade verificada pelo provedor. Trocá-lo
      // pela API contornaria o bloqueio que existia só na tela.
      if (emailNorm !== usuario.email && usuario.provider === 'google') {
        return res.status(403).json({
          sucesso: false,
          mensagem: 'E-mail vinculado ao Google não pode ser alterado.',
        });
      }

      const duplicado = await User.findOne({
        where: {
          email: emailNorm,
          id: { [Op.ne]: usuario.id },
        },
      });
      if (duplicado) {
        return res.status(400).json({
          sucesso: false,
          mensagem: 'Este e-mail já está em uso por outro perfil.',
        });
      }
      usuario.email = emailNorm;
    }

    if (senha) {
      const errosSenha = validarSenha(senha);
      if (errosSenha.length > 0) {
        return res.status(400).json({ sucesso: false, mensagem: errosSenha[0], errosSenha });
      }
      if (senhaAtual) {
        const ok = await usuario.compararSenha(senhaAtual);
        if (!ok) {
          return res.status(400).json({ sucesso: false, mensagem: 'Senha atual incorreta.' });
        }
      }
      usuario.senha = senha;
      // Trocar a senha derruba as outras sessões; esta ganha um token novo.
      usuario.tokenVersion = (usuario.tokenVersion || 0) + 1;
    }

    await usuario.save();

    const trocouSenha = Boolean(senha);
    res.json({
      sucesso: true,
      mensagem: trocouSenha
        ? 'Dados atualizados. As outras sessões foram encerradas.'
        : 'Dados atualizados com sucesso.',
      usuario: usuarioPublico(usuario),
      // Sem token novo o cliente seria deslogado pela própria troca de senha.
      ...(trocouSenha && {
        token: signToken(
          usuario.id,
          usuario.getPerfilEfetivo(),
          usuario.tokenVersion,
          usuario.email,
          usuario.condominioId,
        ),
      }),
    });
  } catch (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      return res.status(400).json({ sucesso: false, mensagem: 'Este e-mail já está em uso por outro perfil.' });
    }
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao atualizar perfil' });
  }
});

router.post('/me/foto', authMiddleware, upload.single('foto'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ sucesso: false, mensagem: 'Nenhum arquivo enviado.' });
    }

    const mime = req.file.mimetype?.toLowerCase();
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(mime)) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Formato não permitido. Envie uma imagem JPG ou PNG.',
      });
    }

    const usuario = await User.findByPk(req.userId);
    const filename = `avatar-${usuario.id}-${Date.now()}.jpg`;
    const filepath = path.join(UPLOAD_DIR, filename);

    await sharp(req.file.buffer)
      .resize(256, 256, { fit: 'cover' })
      .jpeg({ quality: 85 })
      .toFile(filepath);

    if (usuario.fotoUrl) {
      const oldPath = path.join(UPLOAD_DIR, path.basename(usuario.fotoUrl));
      if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
    }

    usuario.fotoUrl = `/uploads/avatars/${filename}`;
    await usuario.save();

    res.json({
      sucesso: true,
      mensagem: 'Foto atualizada com sucesso.',
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    console.error('Erro upload foto:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao enviar foto' });
  }
});

router.post('/forgot-password', forgotLimiter, async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email é obrigatório' });
    }

    const usuario = await User.findOne({ where: { email: email.toLowerCase().trim() } });
    if (!usuario || usuario.provider !== 'local') {
      return res.json({ sucesso: true, mensagem: 'Se esse email estiver cadastrado, você receberá um link em breve.' });
    }

    const { token, hash } = gerarTokenComHash();

    usuario.resetToken = hash;
    usuario.resetTokenExpira = expiraEm(RESET_TOKEN_TTL_MS);
    await usuario.save({ validate: false });

    if (!emailConfigurado()) {
      return res.status(503).json({
        sucesso: false,
        mensagem: 'Serviço de e-mail não configurado. Defina MAIL_USER e MAIL_PASS no .env.',
      });
    }

    await enviarEmailReset(usuario.email, token);

    res.json({ sucesso: true, mensagem: 'Se esse email estiver cadastrado, você receberá um link em breve.' });
  } catch (err) {
    console.error('Erro no forgot-password:', err);
    if (err.message?.includes('e-mail não configurado')) {
      return res.status(503).json({ sucesso: false, mensagem: err.message });
    }
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar solicitação' });
  }
});

router.post('/reset-password/:token', tokenLimiter, async (req, res) => {
  try {
    const { senha } = req.body;
    const errosSenha = validarSenha(senha);
    if (errosSenha.length > 0) {
      return res.status(400).json({ sucesso: false, mensagem: errosSenha[0], errosSenha });
    }

    const usuario = await User.scope('withResetToken').findOne({
      where: {
        resetToken: hashToken(req.params.token),
        resetTokenExpira: { [Op.gt]: new Date() },
      },
    });

    if (!usuario) {
      return res.status(400).json({ sucesso: false, mensagem: 'Token inválido ou expirado' });
    }

    usuario.senha = senha;
    usuario.resetToken = null;
    usuario.resetTokenExpira = null;
    // Quem redefine a senha pode estar expulsando um invasor: derruba tudo.
    usuario.tokenVersion = (usuario.tokenVersion || 0) + 1;
    await usuario.save();

    res.json({ sucesso: true, mensagem: 'Senha redefinida com sucesso. Faça login novamente.' });
  } catch (err) {
    console.error('Erro no reset-password:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao redefinir senha' });
  }
});

router.get('/google/disponivel', (_req, res) => {
  res.json({ disponivel: googleOAuthConfigurado() });
});

router.get('/google', (req, res, next) => {
  if (!googleOAuthConfigurado()) {
    return res.redirect(`${getFrontendUrl()}/login?erro=oauth_nao_configurado`);
  }
  // session: false — a sessão guarda só o `state` do OAuth; a autenticação é JWT.
  passport.authenticate('google', { scope: ['profile', 'email'], session: false })(req, res, next);
});

router.get('/google/callback', (req, res) => {
  passport.authenticate('google', { session: false }, (err, user) => {
    if (err || !user) {
      if (err) console.error('Erro no callback Google OAuth:', err.message || err);
      const codigo = erroOAuthRedirect(err);
      return res.redirect(loginComErroRedirect(codigo));
    }

    if (user.status === STATUS_USUARIO.INACTIVE) {
      return res.redirect(`${getFrontendUrl()}/login?erro=conta_desativada`);
    }
    if (user.status === STATUS_USUARIO.PENDING_ACTIVATION) {
      return res.redirect(`${getFrontendUrl()}/login?erro=cadastro_pendente`);
    }
    if (user.semAcessoSistema) {
      return res.redirect(`${getFrontendUrl()}/login?erro=sem_acesso_sistema`);
    }

    // Em vez de mandar o JWT na URL, entrega um código de uso único e curta
    // duração — a URL vaza para histórico, logs de proxy e cabeçalho Referer.
    (async () => {
      try {
        const { token: codigo, hash } = gerarTokenComHash();
        user.oauthCode = hash;
        user.oauthCodeExpira = expiraEm(OAUTH_CODE_TTL_MS);
        await user.save({ validate: false });

        res.redirect(`${getFrontendUrl()}/auth/callback?code=${codigo}`);
      } catch (e) {
        console.error('Erro ao gerar código OAuth:', e);
        res.redirect(loginComErroRedirect('oauth'));
      }
    })();
  })(req, res);
});

/** Troca o código de uso único do redirect pelo JWT de sessão. */
router.post('/oauth/exchange', tokenLimiter, async (req, res) => {
  try {
    const { code } = req.body;
    if (!code || typeof code !== 'string') {
      return res.status(400).json({ sucesso: false, mensagem: 'Código não fornecido' });
    }

    const usuario = await User.scope('withOauthCode').findOne({
      where: {
        oauthCode: hashToken(code),
        oauthCodeExpira: { [Op.gt]: new Date() },
      },
    });

    if (!usuario) {
      return res.status(400).json({ sucesso: false, mensagem: 'Código inválido ou expirado' });
    }

    // Uso único: consome antes de emitir o token.
    usuario.oauthCode = null;
    usuario.oauthCodeExpira = null;
    await usuario.save({ validate: false });

    const perfil = usuario.getPerfilEfetivo();
    res.json({
      sucesso: true,
      mensagem: 'Login realizado com sucesso',
      token: signToken(usuario.id, perfil, usuario.tokenVersion || 0, usuario.email, usuario.condominioId),
      usuario: usuarioPublico(usuario),
      redirectPath: redirectPorPerfil(perfil),
    });
  } catch (err) {
    console.error('Erro na troca do código OAuth:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao concluir login' });
  }
});

/** Encerra a sessão de verdade: invalida todos os JWT já emitidos. */
router.post('/logout', authMiddleware, async (req, res) => {
  try {
    await req.user.revogarTokens();
    res.json({ sucesso: true, mensagem: 'Sessão encerrada' });
  } catch (err) {
    console.error('Erro no logout:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao encerrar sessão' });
  }
});

export default router;
