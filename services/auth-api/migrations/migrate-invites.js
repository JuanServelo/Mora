import sequelize from '../config/database.js';

export async function garantirColunasInvites() {
  const [[{ exists }]] = await sequelize.query(`
    SELECT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_name = 'invites'
    ) AS exists
  `);

  if (!exists) return;

  await sequelize.query(`
    ALTER TABLE invites
      ADD COLUMN IF NOT EXISTS "responsavelFinanceiro" BOOLEAN NOT NULL DEFAULT false
  `);
}
