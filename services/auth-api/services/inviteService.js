import { Op } from 'sequelize';
import sequelize from '../config/database.js';
import User from '../models/User.js';
import Invite from '../models/Invite.js';
import {
  PERFIS,
  STATUS_CONVITE,
  STATUS_USUARIO,
  CONDOMINIO_DEFAULT,
  perfilExigeUnidade,
  perfilRequerPrecadastro,
  perfilTemAcessoSistema,
} from '../constants/perfis.js';
import { gerarCodigoConvite, normalizarCodigo } from '../utils/inviteCode.js';
import { enviarEmailConvite, AVISO_EMAIL_FALHOU } from '../utils/emailService.js';
import { validarUnidadeExiste } from '../utils/portariaClient.js';
import { normalizarCpf } from '../utils/usuarioPublico.js';

const HORAS_VALIDADE = 48;

export const MSG = {
  CODIGO_INVALIDO: 'Código inválido. Verifique o código recebido ou solicite um novo ao Administrator do condomínio.',
  CODIGO_EXPIRADO: 'Este código expirou. Solicite um novo convite ao Administrator.',
  CODIGO_USADO: 'Este código já foi utilizado. Caso acredite que houve erro, entre em contato com ao Administrator.',
};

function expiraEm48h() {
  return new Date(Date.now() + HORAS_VALIDADE * 60 * 60 * 1000);
}

async function gerarCodigoUnico() {
  for (let i = 0; i < 10; i += 1) {
    const codigo = gerarCodigoConvite();
    const existe = await Invite.findOne({ where: { codigo } });
    if (!existe) return codigo;
  }
  throw new Error('Não foi possível gerar código de convite');
}

export async function buscarConvitePorCodigo(codigoRaw) {
  const codigo = normalizarCodigo(codigoRaw);
  if (!codigo) return { erro: MSG.CODIGO_INVALIDO, tipo: 'invalido' };

  const invite = await Invite.findOne({ where: { codigo } });
  if (!invite) return { erro: MSG.CODIGO_INVALIDO, tipo: 'invalido' };

  if (invite.status === STATUS_CONVITE.USED) {
    return { erro: MSG.CODIGO_USADO, tipo: 'usado' };
  }
  if (invite.status === STATUS_CONVITE.REVOKED) {
    return { erro: MSG.CODIGO_INVALIDO, tipo: 'invalido' };
  }

  const expirado = invite.status === STATUS_CONVITE.EXPIRED
    || (invite.expiresAt && new Date(invite.expiresAt) < new Date());

  if (expirado) {
    if (invite.status === STATUS_CONVITE.PENDING) {
      invite.status = STATUS_CONVITE.EXPIRED;
      await invite.save();
    }
    return { erro: MSG.CODIGO_EXPIRADO, tipo: 'expirado' };
  }

  return { invite };
}

export async function validarConvite(codigoRaw) {
  const resultado = await buscarConvitePorCodigo(codigoRaw);
  if (resultado.erro) {
    return { sucesso: false, mensagem: resultado.erro };
  }

  const { invite } = resultado;
  return {
    sucesso: true,
    convite: {
      email: invite.email,
      perfil: invite.perfil,
      nomePrecadastro: invite.nomePrecadastro,
      cpfPrecadastro: invite.cpfPrecadastro,
      unidadeId: invite.unidadeId,
    },
  };
}

export async function emailEmUsoNoCondominio(email, condominioId, excludeUserId = null) {
  const where = {
    email: email.toLowerCase().trim(),
    condominioId,
    status: STATUS_USUARIO.ACTIVE,
  };
  if (excludeUserId) where.id = { [Op.ne]: excludeUserId };
  const existe = await User.findOne({ where });
  return !!existe;
}

export async function emailEmUsoNaUnidade(email, unidadeId, excludeUserId = null) {
  const where = {
    email: email.toLowerCase().trim(),
    unidadeId,
    status: STATUS_USUARIO.ACTIVE,
  };
  if (excludeUserId) where.id = { [Op.ne]: excludeUserId };
  const existe = await User.findOne({ where });
  return !!existe;
}

export async function emailExisteGlobalmente(email, excludeUserId = null) {
  const where = { email: email.toLowerCase().trim() };
  if (excludeUserId) where.id = { [Op.ne]: excludeUserId };
  const existe = await User.findOne({ where });
  return !!existe;
}

export async function criarConvite({
  email,
  perfil,
  cadastradoPorId,
  condominioId = CONDOMINIO_DEFAULT,
  unidadeId = null,
  nomePrecadastro = null,
  cpfPrecadastro = null,
  responsavelFinanceiro = false,
}) {
  const codigo = await gerarCodigoUnico();
  const invite = await Invite.create({
    codigo,
    email,
    perfil,
    condominioId,
    unidadeId,
    nomePrecadastro,
    cpfPrecadastro: cpfPrecadastro ? normalizarCpf(cpfPrecadastro) : null,
    cadastradoPorId,
    responsavelFinanceiro,
    status: STATUS_CONVITE.PENDING,
    expiresAt: expiraEm48h(),
  });

  const linkAtivacao = `${process.env.FRONTEND_URL || 'http://localhost:5173'}/ativar?codigo=${codigo}`;

  let emailEnviado = false;
  let avisoEmail = null;
  try {
    await enviarEmailConvite(email, codigo, perfil, linkAtivacao);
    emailEnviado = true;
  } catch (err) {
    console.error('[inviteService] Falha ao enviar email:', err.message);
    avisoEmail = AVISO_EMAIL_FALHOU;
  }

  return { invite, emailEnviado, avisoEmail };
}

export async function reenviarConvite(inviteId, cadastradoPorId) {
  const invite = await Invite.findByPk(inviteId);
  if (!invite) return { sucesso: false, mensagem: 'Convite não encontrado', status: 404 };

  if (invite.status === STATUS_CONVITE.USED) {
    return { sucesso: false, mensagem: MSG.CODIGO_USADO, status: 400 };
  }

  if (invite.perfil === PERFIS.CONVIDADO) {
    return { sucesso: false, mensagem: 'Convidados não possuem acesso ao sistema.', status: 400 };
  }

  invite.status = STATUS_CONVITE.REVOKED;
  await invite.save();

  const { invite: novo, emailEnviado, avisoEmail } = await criarConvite({
    email: invite.email,
    perfil: invite.perfil,
    cadastradoPorId,
    condominioId: invite.condominioId,
    unidadeId: invite.unidadeId,
    nomePrecadastro: invite.nomePrecadastro,
    cpfPrecadastro: invite.cpfPrecadastro,
    responsavelFinanceiro: invite.responsavelFinanceiro ?? false,
  });

  return {
    sucesso: true,
    convite: novo,
    emailEnviado,
    avisoEmail,
  };
}

export async function validarDadosConviteAdmin({
  email,
  perfil,
  unidadeId,
  nomePrecadastro,
  cpfPrecadastro,
  condominioId = CONDOMINIO_DEFAULT,
}) {
  if (perfilExigeUnidade(perfil) && !unidadeId) {
    return { sucesso: false, mensagem: 'Unidade é obrigatória para este perfil.' };
  }

  if (perfil === PERFIS.CONVIDADO) {
    return { sucesso: false, mensagem: 'Convidados não possuem acesso ao sistema.' };
  }

  if (unidadeId) {
    const ok = await validarUnidadeExiste(unidadeId);
    if (!ok) {
      return { sucesso: false, mensagem: 'Unidade não encontrada.' };
    }
  }

  if (perfilRequerPrecadastro(perfil)) {
    if (!nomePrecadastro || !cpfPrecadastro) {
      return { sucesso: false, mensagem: 'Nome e CPF são obrigatórios para este perfil.' };
    }
  }

  const emailNorm = email.toLowerCase().trim();

  const emUsoGlobal = await emailExisteGlobalmente(emailNorm);
  if (emUsoGlobal) {
    return { sucesso: false, mensagem: 'Já existe um usuário cadastrado com este e-mail.' };
  }

  return { sucesso: true };
}

export async function residentOwnerAtivoNaUnidade(unidadeId, excludeUserId = null) {
  const where = {
    unidadeId,
    perfil: PERFIS.MORADOR,
    status: STATUS_USUARIO.ACTIVE,
  };
  if (excludeUserId) where.id = { [Op.ne]: excludeUserId };
  return User.findOne({ where });
}

/**
 * Morador ativo da unidade apto a receber a responsabilidade financeira:
 * mora lá e ainda não é o responsável. Antes da simplificação era o Lessee.
 */
export async function moradorSucessorNaUnidade(unidadeId, excludeUserId = null) {
  const where = {
    unidadeId,
    perfil: PERFIS.MORADOR,
    responsavelFinanceiro: false,
    status: STATUS_USUARIO.ACTIVE,
  };
  if (excludeUserId) where.id = { [Op.ne]: excludeUserId };
  return User.findOne({ where });
}

/** Morador que responde financeiramente pela unidade (antigo Lessee). */
export async function lesseeAtivoNaUnidade(unidadeId, excludeUserId = null) {
  const whereUser = {
    unidadeId,
    perfil: PERFIS.MORADOR,
    responsavelFinanceiro: true,
    status: STATUS_USUARIO.ACTIVE,
  };
  if (excludeUserId) whereUser.id = { [Op.ne]: excludeUserId };
  const ativo = await User.findOne({ where: whereUser });
  if (ativo) return ativo;

  // Também bloqueia se já existe convite PENDENTE de responsável nesta unidade
  const convitePendente = await Invite.findOne({
    where: {
      unidadeId,
      perfil: PERFIS.MORADOR,
      responsavelFinanceiro: true,
      status: STATUS_CONVITE.PENDING,
      expiresAt: { [Op.gt]: new Date() },
    },
  });
  return convitePendente || null;
}

export async function ativarConta({
  codigo,
  nome,
  email,
  telefone,
  cpf,
  dataNascimento,
  senha,
}, signToken, usuarioPublicoFn, redirectPorPerfilFn) {
  const resultado = await buscarConvitePorCodigo(codigo);
  if (resultado.erro) {
    return { sucesso: false, mensagem: resultado.erro, status: 400 };
  }

  const { invite } = resultado;
  const emailNorm = email.toLowerCase().trim();
  const cpfNorm = normalizarCpf(cpf);

  // Impede que o código do convite seja usado como nome
  if (nome.trim().toUpperCase() === invite.codigo.toUpperCase()) {
    return {
      sucesso: false,
      mensagem: 'O nome informado parece ser o código do convite. Por favor, informe seu nome completo.',
      status: 400,
    };
  }

  if (invite.nomePrecadastro && invite.nomePrecadastro.trim() !== nome.trim()) {
    return {
      sucesso: false,
      mensagem: 'Os dados informados não correspondem ao convite. Verifique nome e CPF.',
      status: 400,
    };
  }
  if (invite.cpfPrecadastro && invite.cpfPrecadastro !== cpfNorm) {
    return {
      sucesso: false,
      mensagem: 'Os dados informados não correspondem ao convite. Verifique nome e CPF.',
      status: 400,
    };
  }

  const emUso = await emailExisteGlobalmente(emailNorm);
  if (emUso) {
    return {
      sucesso: false,
      mensagem: 'Este e-mail já está em uso. Use um e-mail diferente ou entre em contato com o Administrador.',
      status: 400,
    };
  }

  const transaction = await sequelize.transaction();
  try {
    const inviteLocked = await Invite.findOne({
      where: { id: invite.id, status: STATUS_CONVITE.PENDING },
      lock: transaction.LOCK.UPDATE,
      transaction,
    });

    if (!inviteLocked || new Date(inviteLocked.expiresAt) < new Date()) {
      await transaction.rollback();
      return { sucesso: false, mensagem: MSG.CODIGO_EXPIRADO, status: 400 };
    }

    const usuario = await User.create({
      nome: nome.trim(),
      email: emailNorm,
      telefone: telefone?.trim() || null,
      cpf: cpfNorm,
      dataNascimento: dataNascimento || null,
      senha,
      perfil: invite.perfil,
      status: STATUS_USUARIO.ACTIVE,
      condominioId: invite.condominioId,
      unidadeId: invite.unidadeId,
      cadastradoPorId: invite.cadastradoPorId,
      // Herdado do convite: é o que distingue o morador que paga a fatura.
      responsavelFinanceiro: invite.responsavelFinanceiro ?? false,
      // Perfis sem acesso ao sistema (ex: TERCEIRO, CONVIDADO) nascem bloqueados.
      semAcessoSistema: !perfilTemAcessoSistema(invite.perfil),
      provider: 'local',
      activatedAt: new Date(),
      tokenVersion: 0,
    }, { transaction });

    inviteLocked.status = STATUS_CONVITE.USED;
    inviteLocked.usedAt = new Date();
    inviteLocked.usedByUserId = usuario.id;
    await inviteLocked.save({ transaction });

    await transaction.commit();

    const perfil = usuario.getPerfilEfetivo();
    const token = signToken(usuario.id, perfil, usuario.tokenVersion, usuario.email, usuario.condominioId);

    return {
      sucesso: true,
      mensagem: 'Conta criada com sucesso',
      token,
      usuario: usuarioPublicoFn(usuario),
      redirectPath: redirectPorPerfilFn(perfil),
    };
  } catch (err) {
    await transaction.rollback();
    throw err;
  }
}

export { HORAS_VALIDADE };
