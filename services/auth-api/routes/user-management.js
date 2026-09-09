import express from 'express';
import authMiddleware, { adminMiddleware, gestaoMiddleware } from '../middleware/auth.js';
import { PERFIS, PERFIS_EXIGEM_UNIDADE, podeGerenciarOcupantes, podeGerenciarUsuarios } from '../constants/perfis.js';
import {
  listarUsuariosEscopo,
  emitirConvite,
  reenviarConvite,
  desativarUsuario,
} from '../services/userManagementService.js';
import {
  listarOcupantesUnidade,
  cadastrarLessee,
  cadastrarOccupant,
  cadastrarGuest,
  transferirResponsabilidadeFinanceira,
  verificarElegibilidadeTransferencia,
  removerVinculo,
} from '../services/occupantService.js';
import { validarUnidadeExiste } from '../utils/portariaClient.js';
import User from '../models/User.js';
import Condominio from '../models/Condominio.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';

const router = express.Router();

function occupantUnitMiddleware(req, res, next) {
  const { unidadeId } = req.params;
  const perfil = req.userPerfil;

  if (podeGerenciarOcupantes(perfil)) {
    if (req.user.unidadeId !== unidadeId) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Você não tem permissão para acessar esta unidade.',
      });
    }
  } else if (!podeGerenciarUsuarios(perfil)) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Você não tem permissão para acessar esta funcionalidade.',
    });
  }

  next();
}

router.use(authMiddleware, gestaoMiddleware);

router.get('/users', async (req, res) => {
  try {
    const { usuarios, convitesPendentes } = await listarUsuariosEscopo(req.user, {
      condominioId: req.query.condominioId,
    });

    res.json({
      sucesso: true,
      usuarios: usuarios.map(usuarioPublico),
      convitesPendentes: convitesPendentes.map((c) => ({
        id: c.id,
        email: c.email,
        perfil: c.perfil,
        unidadeId: c.unidadeId,
        status: c.status,
        expiresAt: c.expiresAt,
        createdAt: c.createdAt,
        expirado: new Date(c.expiresAt) < new Date(),
      })),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/invites', async (req, res) => {
  try {
    const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro, condominioId } = req.body;

    if (!email || !perfil) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'E-mail e perfil são obrigatórios.',
      });
    }

    // ADMIN_GERAL pode criar convites para qualquer condomínio ativo.
    // Demais perfis ficam restritos ao seu próprio condominioId.
    const isAdminGeral = req.userPerfil === PERFIS.ADMIN_GERAL;
    const condominioEfetivo = isAdminGeral
      ? (condominioId || req.user.condominioId || null)
      : req.user.condominioId || null;

    if (!condominioEfetivo) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Selecione o condomínio para o qual o convite será emitido.',
      });
    }

    // Scope check: non-ADMIN_GERAL não pode criar usuários em outro condomínio.
    if (!isAdminGeral && condominioId && condominioId !== req.user.condominioId) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Acesso negado: você não tem permissão para criar usuários neste condomínio.',
      });
    }

    // Validar que o condomínio existe e está ativo.
    const condo = await Condominio.findByPk(condominioEfetivo);
    if (!condo) {
      return res.status(400).json({ sucesso: false, mensagem: 'Condomínio não encontrado.' });
    }
    if (condo.status !== 'active') {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Não é possível cadastrar usuários em um condomínio inativo.',
      });
    }

    const resultado = await emitirConvite(req.user, {
      email,
      perfil,
      unidadeId,
      nomePrecadastro,
      cpfPrecadastro,
      condominioId: condominioEfetivo,
    });

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.status(201).json(resultado);
  } catch (err) {
    console.error('Erro emitir convite:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/invites/:id/resend', async (req, res) => {
  try {
    const resultado = await reenviarConvite(Number(req.params.id), req.user.id);

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json({
      sucesso: true,
      mensagem: resultado.emailEnviado
        ? 'Convite reenviado com sucesso.'
        : 'Convite reenviado, mas o e-mail não pôde ser enviado.',
      emailEnviado: resultado.emailEnviado,
      avisoEmail: resultado.avisoEmail || null,
      convite: {
        id: resultado.convite.id,
        email: resultado.convite.email,
        codigo: resultado.convite.codigo,
        expiresAt: resultado.convite.expiresAt,
      },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.patch('/users/:id/deactivate', async (req, res) => {
  try {
    const resultado = await desativarUsuario(req.user, Number(req.params.id));

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.get('/units/:unidadeId/occupants', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await listarOcupantesUnidade(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.get('/units/:unidadeId/transfer-eligibility', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await verificarElegibilidadeTransferencia(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/occupants/lessee', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await cadastrarLessee(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/occupants/occupant', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await cadastrarOccupant(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/occupants/guest', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await cadastrarGuest(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/transfer-financial-responsibility', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await transferirResponsabilidadeFinanceira(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.delete('/units/:unidadeId/occupants/:userId', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await removerVinculo(req.user, req.params.unidadeId, Number(req.params.userId));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Vincular usuário (RESIDENT_OWNER, LESSEE, OCCUPANT) a uma unidade
router.patch('/users/:id/unit', adminMiddleware, async (req, res) => {
  try {
    const { unidadeId } = req.body;

    const alvo = await User.findByPk(Number(req.params.id));
    if (!alvo) return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado.' });

    if (!PERFIS_EXIGEM_UNIDADE.includes(alvo.getPerfilEfetivo())) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Este perfil não pode ser vinculado a uma unidade.',
      });
    }

    if (!unidadeId) return res.status(400).json({ sucesso: false, mensagem: 'unidadeId é obrigatório.' });

    const unidadeValida = await validarUnidadeExiste(unidadeId);
    if (!unidadeValida) return res.status(404).json({ sucesso: false, mensagem: 'Unidade não encontrada.' });

    alvo.unidadeId = unidadeId;
    await alvo.save();

    res.json({ sucesso: true, mensagem: 'Usuário vinculado à unidade.', usuario: usuarioPublico(alvo) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Desvincular usuário de uma unidade
router.delete('/users/:id/unit', adminMiddleware, async (req, res) => {
  try {
    const alvo = await User.findByPk(Number(req.params.id));
    if (!alvo) return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado.' });

    if (alvo.responsavelFinanceiro) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Não é possível desvincular o responsável financeiro da unidade.',
      });
    }

    alvo.unidadeId = null;
    await alvo.save();

    res.json({ sucesso: true, mensagem: 'Vínculo com unidade removido.', usuario: usuarioPublico(alvo) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// Listar usuários vinculados a uma unidade (moradores por unidade)
router.get('/units/:unidadeId/residents', async (req, res) => {
  try {
    const { unidadeId } = req.params;

    const perfil = req.userPerfil;
    const isPM = [PERFIS.ADMIN_GERAL, PERFIS.ADMIN_SINDICO].includes(perfil);
    if (!isPM && req.user.unidadeId !== unidadeId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado.' });
    }

    const moradores = await User.findAll({
      where: { unidadeId, status: 'active' },
      order: [['nome', 'ASC']],
    });

    res.json({ sucesso: true, moradores: moradores.map(usuarioPublico) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
