import { PERFIS } from '../constants/perfis.js';

const ROTA_POR_PERFIL = {
  [PERFIS.PORTEIRO]: '/portaria',
  // Admin Geral opera a plataforma, não um condomínio: painel próprio.
  [PERFIS.ADMIN_GERAL]: '/adm/geral',
  [PERFIS.ADMIN_SINDICO]: '/inicio',
  [PERFIS.MORADOR]: '/inicio',
  [PERFIS.DONO_ALUGUEL]: '/inicio',
  [PERFIS.CONVIDADO]: '/inicio',
  [PERFIS.TERCEIRO]: '/inicio',
};

export function redirectPorPerfil(perfil) {
  return ROTA_POR_PERFIL[perfil] ?? '/inicio';
}

export default redirectPorPerfil;
