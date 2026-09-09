/**
 * Perfis de acesso do Mora.
 *
 * Modelo simplificado: 6 perfis em 3 camadas. Substitui os 11 anteriores, que
 * distinguiam papéis que na prática tinham as mesmas permissões (síndico
 * contratante x operacional, proprietário residente x locatário x ocupante).
 *
 * A distinção entre morador proprietário e inquilino não se perde: ela vive na
 * flag `responsavelFinanceiro` do usuário, que é o que a cobrança precisa saber.
 */

export const PERFIS = {
  /** Cadastra e configura condomínios e empresas. Alcance global. */
  ADMIN_GERAL: 'ADMIN_GERAL',
  /** Gere um condomínio: envia convites, cadastra estrutura e usuários. */
  ADMIN_SINDICO: 'ADMIN_SINDICO',
  /** Opera a guarita: acessos, entregas, visitantes, veículos e chaves. */
  PORTEIRO: 'PORTEIRO',
  /** Mora em uma unidade. Proprietário residente ou inquilino. */
  MORADOR: 'MORADOR',
  /** Dono de imóvel que não mora no condomínio e aluga a unidade. */
  DONO_ALUGUEL: 'DONO_ALUGUEL',
  /** Visitante recorrente pré-autorizado. Sem acesso ao sistema. */
  CONVIDADO: 'CONVIDADO',
  /** Funcionário terceirizado que atua no condomínio. Sem acesso ao sistema por padrão. */
  TERCEIRO: 'TERCEIRO',
};

/** Rótulos para exibição. */
export const PERFIS_LABEL = {
  [PERFIS.ADMIN_GERAL]: 'Admin Geral',
  [PERFIS.ADMIN_SINDICO]: 'Admin Síndico',
  [PERFIS.PORTEIRO]: 'Porteiro',
  [PERFIS.MORADOR]: 'Morador',
  [PERFIS.DONO_ALUGUEL]: 'Dono Aluguel',
  [PERFIS.CONVIDADO]: 'Visitante',
  [PERFIS.TERCEIRO]: 'Terceiro',
};

/* ------------------------------------------------------------- camadas --- */

/** Alcance global: opera sobre todos os condomínios. */
export const PERFIS_PLATAFORMA = [PERFIS.ADMIN_GERAL];

/** Alcance de um condomínio. */
export const PERFIS_CONDOMINIO = [
  PERFIS.ADMIN_SINDICO,
  PERFIS.PORTEIRO,
  PERFIS.TERCEIRO,
];

/** Alcance de uma unidade: exigem `unidadeId`. */
export const PERFIS_UNIDADE = [
  PERFIS.MORADOR,
  PERFIS.DONO_ALUGUEL,
  PERFIS.CONVIDADO,
];

export const PERFIS_EXIGEM_UNIDADE = PERFIS_UNIDADE;

/** Perfis sem login: existem como registro, não como conta. */
export const PERFIS_SEM_ACESSO = [PERFIS.CONVIDADO, PERFIS.TERCEIRO];

/** Quem efetivamente ocupa a unidade — exclui o dono que não mora nela. */
export const PERFIS_OCUPANTE_UNIDADE = [PERFIS.MORADOR, PERFIS.CONVIDADO];

/* --------------------------------------------------------- permissões --- */

/** Matriz: perfil do ator → perfis que ele pode cadastrar. */
export const PERMISSOES_CADASTRO = {
  [PERFIS.ADMIN_GERAL]: [
    PERFIS.ADMIN_GERAL,
    PERFIS.ADMIN_SINDICO,
    PERFIS.PORTEIRO,
    PERFIS.TERCEIRO,
    PERFIS.MORADOR,
    PERFIS.DONO_ALUGUEL,
    PERFIS.CONVIDADO,
  ],
  [PERFIS.ADMIN_SINDICO]: [
    PERFIS.PORTEIRO,
    PERFIS.TERCEIRO,
    PERFIS.MORADOR,
    PERFIS.DONO_ALUGUEL,
    PERFIS.CONVIDADO,
  ],
  // O morador convida quem ocupa a unidade com ele.
  [PERFIS.MORADOR]: [PERFIS.CONVIDADO],
  // O dono não mora, mas cadastra quem vai ocupar o imóvel dele.
  [PERFIS.DONO_ALUGUEL]: [PERFIS.MORADOR, PERFIS.CONVIDADO],
};

/** Acesso ao painel administrativo. */
export const PERFIS_ACESSO_ADMIN = [
  PERFIS.ADMIN_GERAL,
  PERFIS.ADMIN_SINDICO,
];

/** Podem gerenciar usuários (painel de gestão). */
export const PERFIS_GESTAO_USUARIOS = [
  PERFIS.ADMIN_GERAL,
  PERFIS.ADMIN_SINDICO,
  PERFIS.MORADOR,
  PERFIS.DONO_ALUGUEL,
];

/** Operam as funcionalidades de portaria. */
export const PERFIS_PORTARIA = [
  PERFIS.ADMIN_GERAL,
  PERFIS.ADMIN_SINDICO,
  PERFIS.PORTEIRO,
];

/* ------------------------------------------------------------- estados --- */

export const STATUS_USUARIO = {
  PENDING_ACTIVATION: 'pending_activation',
  ACTIVE: 'active',
  INACTIVE: 'inactive',
};

export const STATUS_CONVITE = {
  PENDING: 'pending',
  USED: 'used',
  EXPIRED: 'expired',
  REVOKED: 'revoked',
};

export const CONDOMINIO_DEFAULT = 'default';

/* ------------------------------------------------------------- helpers --- */

export function podeCadastrarPerfil(perfilAtor, perfilAlvo) {
  return PERMISSOES_CADASTRO[perfilAtor]?.includes(perfilAlvo) ?? false;
}

export function perfilExigeUnidade(perfil) {
  return PERFIS_EXIGEM_UNIDADE.includes(perfil);
}

/** Perfis de unidade são pré-cadastrados com nome e CPF antes do convite. */
export function perfilRequerPrecadastro(perfil) {
  return PERFIS_UNIDADE.includes(perfil);
}

export function podeAcessarAdmin(perfil) {
  return PERFIS_ACESSO_ADMIN.includes(perfil);
}

export function podeGerenciarUsuarios(perfil) {
  return PERFIS_GESTAO_USUARIOS.includes(perfil);
}

/** Quem responde pelos ocupantes de uma unidade. */
export function podeGerenciarOcupantes(perfil) {
  return [PERFIS.MORADOR, PERFIS.DONO_ALUGUEL].includes(perfil);
}

export function podeAcessarPortaria(perfil) {
  return PERFIS_PORTARIA.includes(perfil);
}

export function isPerfilCondominio(perfil) {
  return [...PERFIS_PLATAFORMA, ...PERFIS_CONDOMINIO].includes(perfil);
}

export function perfilTemAcessoSistema(perfil) {
  return !PERFIS_SEM_ACESSO.includes(perfil);
}

/* --------------------------------------------------------- compatib. --- */

/**
 * De-para dos 11 perfis antigos. Usado pela migração e para aceitar tokens
 * emitidos antes da simplificação, que ainda carregam o valor antigo.
 */
export const PERFIL_LEGADO_PARA_NOVO = {
  CONTRACTING_PROPERTY_MANAGER: PERFIS.ADMIN_GERAL,
  CONTRACTING_SYNDIC: PERFIS.ADMIN_GERAL,
  OPERATIONAL_SYNDIC: PERFIS.ADMIN_SINDICO,
  ADMINISTRATOR: PERFIS.ADMIN_SINDICO,
  DOORMAN: PERFIS.PORTEIRO,
  REAL_ESTATE_AGENCY: PERFIS.ADMIN_SINDICO,
  RESIDENT_OWNER: PERFIS.MORADOR,
  LESSEE: PERFIS.MORADOR,
  OCCUPANT: PERFIS.MORADOR,
  ABSENT_OWNER: PERFIS.DONO_ALUGUEL,
  GUEST: PERFIS.CONVIDADO,
};

/** Normaliza um perfil, traduzindo valores legados quando aparecerem. */
export function normalizarPerfil(perfil) {
  if (!perfil) return null;
  if (PERFIS[perfil]) return perfil;
  return PERFIL_LEGADO_PARA_NOVO[perfil] ?? null;
}
