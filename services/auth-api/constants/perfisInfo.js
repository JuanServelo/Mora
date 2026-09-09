import { PERFIS } from './perfis.js';

/**
 * Descrição de cada perfil, consumida pela tela de perfis (routes/perfis.js).
 * A fonte de verdade das permissões é constants/perfis.js — aqui é a versão
 * legível para o usuário final.
 */
export const PERFIS_DESCRICAO = {
  [PERFIS.ADMIN_GERAL]: {
    label: 'Admin Geral',
    categoria: 'plataforma',
    descricao: 'Responsável pela plataforma. Cadastra e configura condomínios e empresas.',
    permissoes: [
      'Cadastrar e configurar condomínios e empresas',
      'Cadastrar síndicos em qualquer condomínio',
      'Visualizar todos os usuários da plataforma',
      'Gerenciar as configurações do sistema',
    ],
    podeCadastrar: [
      PERFIS.ADMIN_GERAL,
      PERFIS.ADMIN_SINDICO,
      PERFIS.PORTEIRO,
      PERFIS.MORADOR,
      PERFIS.DONO_ALUGUEL,
      PERFIS.CONVIDADO,
    ],
  },

  [PERFIS.ADMIN_SINDICO]: {
    label: 'Admin Síndico',
    categoria: 'condominio',
    descricao: 'Síndico responsável pela gestão de um condomínio e pelo envio de convites.',
    permissoes: [
      'Enviar convites e gerenciar os usuários do condomínio',
      'Cadastrar blocos, apartamentos e áreas comuns',
      'Publicar avisos e convocar assembleias',
      'Acompanhar a operação da portaria',
    ],
    podeCadastrar: [
      PERFIS.PORTEIRO,
      PERFIS.MORADOR,
      PERFIS.DONO_ALUGUEL,
      PERFIS.CONVIDADO,
    ],
  },

  [PERFIS.PORTEIRO]: {
    label: 'Porteiro',
    categoria: 'condominio',
    descricao: 'Opera a guarita: controla entradas, saídas e o que passa pela portaria.',
    permissoes: [
      'Registrar entradas e saídas de acesso',
      'Registrar visitantes e conferir pré-autorizações',
      'Receber e dar baixa em encomendas',
      'Controlar veículos, vagas e retirada de chaves',
    ],
    podeCadastrar: [],
  },

  [PERFIS.MORADOR]: {
    label: 'Morador',
    categoria: 'unidade',
    descricao: 'Mora em uma unidade do condomínio, como proprietário residente ou inquilino.',
    permissoes: [
      'Consultar avisos, atas e a base de conhecimento',
      'Reservar áreas comuns',
      'Abrir e acompanhar ocorrências',
      'Pré-autorizar visitantes da sua unidade',
      'Consultar as faturas da unidade',
    ],
    podeCadastrar: [PERFIS.CONVIDADO],
  },

  [PERFIS.DONO_ALUGUEL]: {
    label: 'Dono Aluguel',
    categoria: 'unidade',
    descricao: 'Proprietário que não mora no condomínio e aluga o imóvel a um morador.',
    permissoes: [
      'Cadastrar o morador que ocupa o imóvel',
      'Acompanhar as faturas da unidade',
      'Consultar avisos e atas',
    ],
    podeCadastrar: [PERFIS.MORADOR, PERFIS.CONVIDADO],
  },

  [PERFIS.CONVIDADO]: {
    label: 'Convidado',
    categoria: 'unidade',
    descricao: 'Visitante recorrente pré-autorizado por um morador. Não acessa o sistema.',
    permissoes: [
      'Entrada liberada na portaria durante a validade da autorização',
    ],
    podeCadastrar: [],
    semAcessoSistema: true,
  },

  [PERFIS.TERCEIRO]: {
    label: 'Terceiro',
    categoria: 'condominio',
    descricao: 'Funcionário terceirizado que atua no condomínio (limpeza, manutenção, segurança). Não acessa o sistema por padrão.',
    permissoes: [
      'Pode receber a posse de chaves do condomínio registradas pelo porteiro',
    ],
    podeCadastrar: [],
    semAcessoSistema: true,
  },
};

export const CATEGORIAS_PERFIS = {
  plataforma: {
    label: 'Nível Plataforma',
    descricao: 'Gerenciam múltiplos condomínios',
  },
  condominio: {
    label: 'Nível Condomínio',
    descricao: 'Atuam dentro de um condomínio específico',
  },
  unidade: {
    label: 'Nível Unidade',
    descricao: 'Vinculados a uma unidade (apartamento/casa)',
  },
};
