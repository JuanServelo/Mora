import { Sequelize } from 'sequelize';

const sequelize = new Sequelize('auth_db', 'admin', 'Vibers@2112', {
  host: 'localhost',
  port: 5433,
  dialect: 'postgres',
  logging: false,
});

async function run() {
  try {
    await sequelize.authenticate();
    const [results] = await sequelize.query(`SELECT email, "condominioId" FROM users WHERE email='sindico@mora.com'`);
    console.log('CondominioId:', results[0].condominioId);
    
    // Now call plan-service directly to see what it returns
    const axios = (await import('axios')).default;
    const res = await axios.get(`http://localhost:8093/api/subscriptions/condominium/${results[0].condominioId}`);
    console.log('Subscription API response:', JSON.stringify(res.data, null, 2));
    
  } catch (err) {
    console.error('Erro:', err.message);
  } finally {
    await sequelize.close();
  }
}

run();
