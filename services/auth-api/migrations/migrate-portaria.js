import sequelize from '../config/database.js';

export async function garantirTabelaPortaria() {
  await sequelize.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS "entradaPermitida" BOOLEAN DEFAULT false
  `);

  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS registros_acesso (
      id SERIAL PRIMARY KEY,
      "usuarioId"       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      tipo              VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
      "registradoPorId" INTEGER REFERENCES users(id) ON DELETE SET NULL,
      "condominioId"    VARCHAR(50),
      "createdAt"       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  // Tabelas criadas antes da adição de condominioId não têm essa coluna;
  // sem ela, todas as queries que filtram por condomínio retornam 500.
  await sequelize.query(`
    ALTER TABLE registros_acesso
      ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50),
      ADD COLUMN IF NOT EXISTS "nomeSnapshot" VARCHAR(150),
      ADD COLUMN IF NOT EXISTS "perfilSnapshot" VARCHAR(50)
  `);

  await sequelize.query(`
    CREATE INDEX IF NOT EXISTS idx_registros_acesso_usuario
      ON registros_acesso ("usuarioId", "createdAt" DESC)
  `);
}
