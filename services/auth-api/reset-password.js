import { Sequelize } from 'sequelize';
import bcrypt from 'bcryptjs';

const sequelize = new Sequelize('auth_db', 'admin', 'Vibers@2112', {
  host: 'localhost',
  port: 5433,
  dialect: 'postgres',
  logging: false,
});

async function run() {
  try {
    await sequelize.authenticate();
    const hash = await bcrypt.hash('123456', 10);
    await sequelize.query(`UPDATE users SET senha = '${hash}' WHERE email = 'sindico@mora.com'`);
    console.log('Senha atualizada com sucesso!');
  } catch (err) {
    console.error('Erro:', err);
  } finally {
    await sequelize.close();
  }
}

run();
