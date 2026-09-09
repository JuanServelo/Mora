import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8094',
  headers: { 'Content-Type': 'application/json' },
});

// Injeta JWT do localStorage em todas as requisições
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const conhecimentoApi = {
  listarTodos: () => api.get('/artigos'),
  listarPublicados: () => api.get('/artigos?publicadosOnly=true'),
  listarPorCategoria: (categoria) => api.get(`/artigos?categoria=${categoria}`),
  buscarPorTitulo: (titulo) => api.get(`/artigos/buscar`, { params: { titulo } }),
  buscarPorId: (id) => api.get(`/artigos/${id}`),
  criar: (dados) => api.post('/artigos', dados),
  atualizar: (id, dados) => api.put(`/artigos/${id}`, dados),
  excluir: (id) => api.delete(`/artigos/${id}`),
};

export default api;
