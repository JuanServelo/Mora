import sequelize from '../config/database.js';

/**
 * Adiciona o perfil TERCEIRO ao banco.
 *
 * Em instalações frescas (init-databases.sql), `perfil` é VARCHAR — aceita
 * qualquer string sem migração. Quando o banco passou pela migrate-perfis-v2,
 * a coluna vira um tipo ENUM e precisa de ALTER TYPE para aceitar novos valores.
 *
 * Idempotente: ADD VALUE IF NOT EXISTS não falha se o valor já existir.
 */
export async function migrarPerfilTerceiro() {
  const [[v2]] = await sequelize.query(`
    SELECT EXISTS (
      SELECT 1 FROM pg_type WHERE typname = 'enum_users_perfil_v2'
    ) AS exists
  `);

  if (!v2?.exists) return; // Coluna ainda é VARCHAR — nenhuma ação necessária

  await sequelize.query(
    `ALTER TYPE enum_users_perfil_v2 ADD VALUE IF NOT EXISTS 'TERCEIRO'`,
  );
  await sequelize.query(
    `ALTER TYPE enum_invites_perfil_v2 ADD VALUE IF NOT EXISTS 'TERCEIRO'`,
  );

  await sequelize.query(`
    UPDATE users
    SET "semAcessoSistema" = true
    WHERE perfil = 'TERCEIRO'
      AND "semAcessoSistema" IS DISTINCT FROM true
  `);
}
