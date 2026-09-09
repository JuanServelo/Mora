import express from 'express';
import { Op } from 'sequelize';
import sequelize from '../config/database.js';
import authMiddleware from '../middleware/auth.js';
import User from '../models/User.js';
import RegistroAcesso from '../models/RegistroAcesso.js';
import { PERFIS, PERFIS_LABEL, STATUS_USUARIO, podeAcessarAdmin } from '../constants/perfis.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';

const router = express.Router();

router.use(authMiddleware);

function toPerfilLabel(s) {
  return PERFIS_LABEL[s] ?? s ?? '—';
}

function portariaMiddleware(req, res, next) {
  const p = req.userPerfil;
  if (p === PERFIS.PORTEIRO || podeAcessarAdmin(p)) return next();
  return res.status(403).json({ sucesso: false, mensagem: 'Apenas porteiros têm acesso a esta funcionalidade.' });
}

function responsavelMiddleware(req, res, next) {
  const p = req.userPerfil;
  const permitidos = [
    PERFIS.MORADOR, PERFIS.DONO_ALUGUEL,
    PERFIS.ADMIN_GERAL, PERFIS.ADMIN_SINDICO,
  ];
  if (permitidos.includes(p)) return next();
  return res.status(403).json({ sucesso: false, mensagem: 'Sem permissão para gerenciar convidados.' });
}

async function ultimoRegistroPorUsuario(usuarioIds, condominioId) {
  if (!usuarioIds.length) return {};
  const rows = await sequelize.query(`
    SELECT DISTINCT ON ("usuarioId") "usuarioId", tipo, "createdAt"
    FROM registros_acesso
    WHERE "usuarioId" IN (:ids) AND "condominioId" = :condominioId
    ORDER BY "usuarioId", "createdAt" DESC
  `, {
    replacements: { ids: usuarioIds, condominioId },
    type: sequelize.QueryTypes.SELECT,
  });
  return Object.fromEntries(rows.map((r) => [r.usuarioId, r]));
}

function mapUsuario(u, ultimoRegistro) {
  const pub = usuarioPublico(u);
  const ultimo = ultimoRegistro?.[u.id];
  return {
    ...pub,
    statusAcesso: ultimo?.tipo === 'ENTRADA' ? 'DENTRO' : 'FORA',
    ultimoRegistroEm: ultimo?.createdAt ?? null,
  };
}

// ─── PORTEIRO: listar convidados (GUEST) do condomínio ───
router.get('/guests', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const guests = await User.findAll({
      where: { condominioId, perfil: PERFIS.CONVIDADO, status: STATUS_USUARIO.ACTIVE },
      order: [['nome', 'ASC']],
    });
    const ids = guests.map((g) => g.id);
    const ultimos = await ultimoRegistroPorUsuario(ids, condominioId);
    res.json({ sucesso: true, guests: guests.map((g) => mapUsuario(g, ultimos)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: listar residentes (todos exceto GUEST) do condomínio ───
router.get('/residentes', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const residentes = await User.findAll({
      where: {
        condominioId,
        status: STATUS_USUARIO.ACTIVE,
        perfil: { [Op.ne]: PERFIS.CONVIDADO },
        semAcessoSistema: false,
      },
      order: [['nome', 'ASC']],
    });
    const ids = residentes.map((u) => u.id);
    const ultimos = await ultimoRegistroPorUsuario(ids, condominioId);
    res.json({ sucesso: true, residentes: residentes.map((u) => mapUsuario(u, ultimos)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: quem está dentro agora (detalhado) ───
router.get('/dentro', portariaMiddleware, async (req, res) => {
  try {
    const { condominioId } = req.user;
    const dentro = await sequelize.query(`
      WITH latest AS (
        SELECT DISTINCT ON ("usuarioId")
          id, "usuarioId", tipo, "createdAt", "registradoPorId", "nomeSnapshot", "perfilSnapshot"
        FROM registros_acesso
        WHERE "condominioId" = :condominioId
        ORDER BY "usuarioId", "createdAt" DESC
      )
      SELECT
        l.id           AS "entradaId",
        l."usuarioId",
        l."createdAt"  AS "entradaEm",
        COALESCE(l."nomeSnapshot", u.nome)         AS nome,
        COALESCE(l."perfilSnapshot", u.perfil::text) AS perfil,
        u."unidadeId",
        rb.nome AS "registradoPorNome"
      FROM latest l
      LEFT JOIN users u  ON u.id = l."usuarioId"
      LEFT JOIN users rb ON rb.id = l."registradoPorId"
      WHERE l.tipo = 'ENTRADA'
      ORDER BY l."createdAt" DESC
    `, { replacements: { condominioId }, type: sequelize.QueryTypes.SELECT });

    res.json({ sucesso: true, dentro });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: registrar entrada ───
router.post('/entrada/:userId', portariaMiddleware, async (req, res) => {
  try {
    const alvo = await User.findByPk(Number(req.params.userId));
    if (!alvo || alvo.status !== STATUS_USUARIO.ACTIVE) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado ou inativo.' });
    }

    if (alvo.condominioId !== req.user.condominioId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Usuário não pertence a este condomínio.' });
    }

    if (alvo.perfil === PERFIS.CONVIDADO && !alvo.entradaPermitida) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Entrada não permitida. O responsável da unidade não autorizou a entrada deste convidado.',
      });
    }

    await RegistroAcesso.create({
      usuarioId: alvo.id,
      tipo: 'ENTRADA',
      registradoPorId: req.user.id,
      condominioId: req.user.condominioId,
      nomeSnapshot: alvo.nome,
      perfilSnapshot: toPerfilLabel(alvo.perfil),
    });

    res.json({ sucesso: true, mensagem: `Entrada de ${alvo.nome} registrada.` });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: registrar saída ───
router.post('/saida/:userId', portariaMiddleware, async (req, res) => {
  try {
    const alvo = await User.findByPk(Number(req.params.userId));
    if (!alvo || alvo.status !== STATUS_USUARIO.ACTIVE) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado ou inativo.' });
    }

    if (alvo.condominioId !== req.user.condominioId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Usuário não pertence a este condomínio.' });
    }

    await RegistroAcesso.create({
      usuarioId: alvo.id,
      tipo: 'SAIDA',
      registradoPorId: req.user.id,
      condominioId: req.user.condominioId,
    });

    res.json({ sucesso: true, mensagem: `Saída de ${alvo.nome} registrada.` });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── RESPONSÁVEL: ver meus convidados ───
router.get('/meus-guests', responsavelMiddleware, async (req, res) => {
  try {
    const { unidadeId, condominioId, id: userId } = req.user;

    const where = {
      perfil: PERFIS.CONVIDADO,
      status: STATUS_USUARIO.ACTIVE,
      condominioId,
    };

    if (unidadeId) {
      where.unidadeId = unidadeId;
    } else {
      where.cadastradoPorId = userId;
    }

    const guests = await User.findAll({ where, order: [['nome', 'ASC']] });
    const ids = guests.map((g) => g.id);
    const ultimos = await ultimoRegistroPorUsuario(ids, condominioId);

    res.json({ sucesso: true, guests: guests.map((g) => mapUsuario(g, ultimos)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── RESPONSÁVEL: alternar permissão de entrada do convidado ───
router.patch('/guests/:guestId/permissao', responsavelMiddleware, async (req, res) => {
  try {
    const guest = await User.findByPk(Number(req.params.guestId));
    if (!guest || guest.perfil !== PERFIS.CONVIDADO || guest.status !== STATUS_USUARIO.ACTIVE) {
      return res.status(404).json({ sucesso: false, mensagem: 'Convidado não encontrado.' });
    }

    if (guest.condominioId !== req.user.condominioId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Convidado não pertence a este condomínio.' });
    }

    const p = req.userPerfil;
    const isAdmin = podeAcessarAdmin(p);

    if (!isAdmin) {
      const responsavel = req.user.responsavelFinanceiro && req.user.unidadeId === guest.unidadeId;
      const criador = guest.cadastradoPorId === req.user.id;
      if (!responsavel && !criador) {
        return res.status(403).json({
          sucesso: false,
          mensagem: 'Apenas o responsável da unidade pode autorizar a entrada de convidados.',
        });
      }
    }

    const { permitir } = req.body;
    guest.entradaPermitida = typeof permitir === 'boolean' ? permitir : !guest.entradaPermitida;
    await guest.save();

    const acao = guest.entradaPermitida ? 'autorizada' : 'bloqueada';
    res.json({
      sucesso: true,
      mensagem: `Entrada de ${guest.nome} ${acao}.`,
      entradaPermitida: guest.entradaPermitida,
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: listar todos os usuários do condomínio ───
router.get('/usuarios-condominio', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const usuarios = await User.findAll({
      where: { condominioId, status: STATUS_USUARIO.ACTIVE },
      order: [['nome', 'ASC']],
    });
    res.json({ sucesso: true, usuarios: usuarios.map(usuarioPublico) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: histórico de entradas e saídas do condomínio ───
router.get('/historico-acesso', portariaMiddleware, async (req, res) => {
  try {
    const { condominioId } = req.user;
    const { nome, perfil, status, dataInicio, dataFim } = req.query;

    const conditions = [`e.tipo = 'ENTRADA'`, `e."condominioId" = :condominioId`];
    const replacements = { condominioId };

    if (nome) {
      conditions.push(`COALESCE(e."nomeSnapshot", u.nome) ILIKE :nomeLike`);
      replacements.nomeLike = `%${nome}%`;
    }
    if (perfil) {
      // 'Visitante' covers both new label and legacy 'Convidado' snapshot
      if (perfil === 'Visitante') {
        conditions.push(`COALESCE(e."perfilSnapshot", u.perfil::text) IN ('Visitante', 'Convidado')`);
      } else {
        conditions.push(`COALESCE(e."perfilSnapshot", u.perfil::text) = :perfil`);
        replacements.perfil = perfil;
      }
    }
    if (dataFim) {
      conditions.push(`e."createdAt" <= :dataFim`);
      replacements.dataFim = new Date(`${dataFim}T23:59:59Z`);
    }

    const rows = await sequelize.query(`
      WITH pares AS (
        SELECT
          e.id,
          e."usuarioId",
          e."createdAt"   AS "entradaEm",
          COALESCE(e."nomeSnapshot", u.nome)           AS nome,
          COALESCE(e."perfilSnapshot", u.perfil::text) AS perfil,
          u."unidadeId",
          rb.nome AS "registradoPorNome",
          (
            SELECT s."createdAt"
            FROM registros_acesso s
            WHERE s."usuarioId"    = e."usuarioId"
              AND s."condominioId" = e."condominioId"
              AND s.tipo           = 'SAIDA'
              AND s."createdAt"    > e."createdAt"
            ORDER BY s."createdAt" ASC
            LIMIT 1
          ) AS "saidaEm"
        FROM registros_acesso e
        LEFT JOIN users u  ON u.id = e."usuarioId"
        LEFT JOIN users rb ON rb.id = e."registradoPorId"
        WHERE ${conditions.join(' AND ')}
      )
      SELECT * FROM pares
      WHERE TRUE
        ${status === 'DENTRO' ? 'AND "saidaEm" IS NULL' : ''}
        ${status === 'SAIU'   ? 'AND "saidaEm" IS NOT NULL' : ''}
        ${dataInicio ? `AND ("saidaEm" >= :dataInicio OR "saidaEm" IS NULL)` : ''}
      ORDER BY CASE WHEN "saidaEm" IS NULL THEN 0 ELSE 1 END, "entradaEm" DESC
      LIMIT 200
    `, { replacements: { ...replacements, ...(dataInicio ? { dataInicio: new Date(`${dataInicio}T00:00:00Z`) } : {}) },
         type: sequelize.QueryTypes.SELECT });

    res.json({ sucesso: true, registros: rows });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: histórico de acessos de um usuário ───
router.get('/historico/:userId', portariaMiddleware, async (req, res) => {
  try {
    const registros = await RegistroAcesso.findAll({
      where: {
        usuarioId: Number(req.params.userId),
        condominioId: req.user.condominioId,
      },
      order: [['createdAt', 'DESC']],
      limit: 50,
    });
    res.json({ sucesso: true, registros });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
