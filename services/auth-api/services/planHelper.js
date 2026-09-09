import axios from 'axios';

export async function fetchActiveModules(condominioId) {
  if (!condominioId) return [];
  try {
    const url = process.env.PLAN_SERVICE_URL || 'http://plan-service:8093';
    const res = await axios.get(`${url}/api/subscriptions/condominium/${condominioId}`);
    if (res.data && res.data.length > 0) {
      const activeSub = res.data.find(sub => sub.status === 'ACTIVE');
      if (activeSub && activeSub.plan) {
        return activeSub.plan.activeModules || [];
      }
    }
  } catch (err) {
    console.error('Erro ao buscar modulos do plano:', err.message);
  }
  return [];
}
