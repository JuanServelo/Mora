import express from 'express';
import Condominio from '../models/Condominio.js';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { PERFIS, STATUS_USUARIO } from '../constants/perfis.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';

const router = express.Router();

function gerenteMiddleware(req, res, next) {
  const { userPerfil } = req;
  const permitidos = [PERFIS.ADMIN_GERAL];
  if (!permitidos.includes(userPerfil)) {
    return res.status(403).json({ sucesso: false, mensagem: 'Apenas administradoras ou síndicos contratantes podem gerenciar clientes.' });
  }
  next();
}

router.use(authMiddleware);

// Listar condomínios
router.get('/', async (req, res) => {
  try {
    const isPM = req.userPerfil === PERFIS.ADMIN_GERAL;

    const where = isPM ? {} : { id: req.user.condominioId };

    // Filtro de status opcional (ex: ?status=active para o seletor de criação de usuários).
    if (req.query.status) where.status = req.query.status;

    const condominios = await Condominio.findAll({
      where,
      order: [['nome', 'ASC']],
    });

    res.json({ sucesso: true, condominios });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Buscar um condomínio
router.get('/:id', async (req, res) => {
  try {
    const cond = await Condominio.findByPk(req.params.id);
    if (!cond) return res.status(404).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });

    const isPM = req.userPerfil === PERFIS.ADMIN_GERAL;
    if (!isPM && req.user.condominioId !== cond.id) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado.' });
    }

    res.json({ sucesso: true, condominio: cond });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Criar condomínio (apenas CONTRACTING_PROPERTY_MANAGER)
router.post('/', adminMiddleware, gerenteMiddleware, async (req, res) => {
  try {
    const { id, nome, cnpj, endereco, telefone, email } = req.body;

    if (!id || !nome) {
      return res.status(400).json({ sucesso: false, mensagem: 'ID e nome são obrigatórios.' });
    }

    if (!/^[a-z0-9_-]+$/i.test(id)) {
      return res.status(400).json({ sucesso: false, mensagem: 'ID deve conter apenas letras, números, hífens e underscores.' });
    }

    const existe = await Condominio.findByPk(id);
    if (existe) return res.status(409).json({ sucesso: false, mensagem: 'Já existe um condomínio com este ID.' });

    const condominio = await Condominio.create({
      id,
      nome,
      cnpj: cnpj || null,
      endereco: endereco || null,
      telefone: telefone || null,
      email: email || null,
      status: 'active',
      criadoPorId: req.user.id,
    });

    res.status(201).json({ sucesso: true, condominio });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Atualizar condomínio
router.put('/:id', adminMiddleware, async (req, res) => {
  try {
    const cond = await Condominio.findByPk(req.params.id);
    if (!cond) return res.status(404).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });

    const isPM = req.userPerfil === PERFIS.ADMIN_GERAL;
    if (!isPM && req.user.condominioId !== cond.id) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado.' });
    }

    const { nome, cnpj, endereco, telefone, email } = req.body;
    if (nome) cond.nome = nome;
    if (cnpj !== undefined) cond.cnpj = cnpj;
    if (endereco !== undefined) cond.endereco = endereco;
    if (telefone !== undefined) cond.telefone = telefone;
    if (email !== undefined) cond.email = email;

    await cond.save();
    res.json({ sucesso: true, condominio: cond });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Desativar condomínio
router.patch('/:id/deactivate', adminMiddleware, gerenteMiddleware, async (req, res) => {
  try {
    const cond = await Condominio.findByPk(req.params.id);
    if (!cond) return res.status(404).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });

    cond.status = 'inactive';
    await cond.save();
    res.json({ sucesso: true, mensagem: 'Condomínio desativado.' });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Reativar condomínio
router.patch('/:id/activate', adminMiddleware, gerenteMiddleware, async (req, res) => {
  try {
    const cond = await Condominio.findByPk(req.params.id);
    if (!cond) return res.status(404).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });

    cond.status = 'active';
    await cond.save();
    res.json({ sucesso: true, mensagem: 'Condomínio reativado.' });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Listar usuários de um condomínio
router.get('/:id/users', adminMiddleware, async (req, res) => {
  try {
    const cond = await Condominio.findByPk(req.params.id);
    if (!cond) return res.status(404).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });

    const isPM = req.userPerfil === PERFIS.ADMIN_GERAL;
    if (!isPM && req.user.condominioId !== cond.id) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado.' });
    }

    const usuarios = await User.findAll({
      where: { condominioId: cond.id },
      order: [['nome', 'ASC']],
    });

    res.json({ sucesso: true, condominio: cond, usuarios: usuarios.map(usuarioPublico) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Vincular usuário a um condomínio
router.patch('/:id/assign-user', adminMiddleware, async (req, res) => {
  try {
    const cond = await Condominio.findByPk(req.params.id);
    if (!cond) return res.status(404).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });

    const isPM = req.userPerfil === PERFIS.ADMIN_GERAL;
    if (!isPM && req.user.condominioId !== cond.id) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado.' });
    }

    const { userId } = req.body;
    if (!userId) return res.status(400).json({ sucesso: false, mensagem: 'userId é obrigatório.' });

    const alvo = await User.findByPk(userId);
    if (!alvo) return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado.' });

    alvo.condominioId = cond.id;
    await alvo.save();

    res.json({ sucesso: true, mensagem: 'Usuário vinculado ao condomínio.', usuario: usuarioPublico(alvo) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
