/**
 * Seed de desenvolvimento — cria um condomínio completo com um usuário de cada
 * perfil, apartamentos, frações ideais, tipo de taxa e regras financeiras.
 *
 * Uso: node C:\Mora\scripts\seed-dev.mjs
 */

import pg from 'pg';
import bcrypt from 'bcryptjs';

const AUTH_DB = {
  host: 'localhost', port: 5432, database: 'auth_db',
  user: 'postgres', password: 'Man3craft!',
};

const AUTH_API       = 'http://localhost:3001';
const PORTARIA_API   = 'http://localhost:8090';
const FINANCEIRO_API = 'http://localhost:3004';

const COND_ID   = 'cond-seed-001';
const COND_NOME = 'Residencial Parque Verde (Teste)';
const SENHA     = 'Mora@2024';

/* ─── helpers ──────────────────────────────────────────────────────────── */

function log(emoji, msg) { console.log(`${emoji}  ${msg}`); }

async function api(url, opts = {}) {
  const { headers: extraHeaders, body, ignorarErro, ...restOpts } = opts;
  const r = await fetch(url, {
    ...restOpts,
    headers: { 'Content-Type': 'application/json', ...(extraHeaders || {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await r.json().catch(() => ({}));
  if (!r.ok && !ignorarErro) {
    throw new Error(`${opts.method || 'GET'} ${url} → ${r.status}: ${json.mensagem || JSON.stringify(json)}`);
  }
  return json;
}

function bearer(token) { return { Authorization: `Bearer ${token}` }; }

/* ─── 1. Usuários no banco ─────────────────────────────────────────────── */

async function criarUsuarios() {
  log('🔑', 'Criando usuários no auth_db...');
  const pool = new pg.Pool(AUTH_DB);
  const hash = await bcrypt.hash(SENHA, 10);
  const agora = new Date().toISOString();

  const usuarios = [
    { nome: 'Admin Geral',   email: 'admin@mora.test',    perfil: 'ADMIN_GERAL',    condominioId: null,      unidadeId: null, responsavelFinanceiro: false, cpf: '000.000.000-00' },
    { nome: 'Carlos Síndico',email: 'sindico@mora.test',  perfil: 'ADMIN_SINDICO',  condominioId: COND_ID,   unidadeId: null, responsavelFinanceiro: false, cpf: '111.111.111-11' },
    { nome: 'José Porteiro', email: 'porteiro@mora.test', perfil: 'PORTEIRO',       condominioId: COND_ID,   unidadeId: null, responsavelFinanceiro: false, cpf: '222.222.222-22' },
    { nome: 'Ana Moradora',  email: 'morador@mora.test',  perfil: 'MORADOR',        condominioId: COND_ID,   unidadeId: null, responsavelFinanceiro: true,  cpf: '333.333.333-33' },
    { nome: 'Paulo Dono',    email: 'dono@mora.test',     perfil: 'DONO_ALUGUEL',   condominioId: COND_ID,   unidadeId: null, responsavelFinanceiro: true,  cpf: '444.444.444-44' },
  ];

  const ids = {};
  for (const u of usuarios) {
    const { rows } = await pool.query(
      `INSERT INTO users
         (nome, email, senha, perfil, status, "condominioId", "unidadeId",
          "responsavelFinanceiro", cpf, "tokenVersion", "createdAt", "updatedAt", "activatedAt")
       VALUES ($1,$2,$3,$4,'active',$5,$6,$7,$8,0,$9,$9,$9)
       ON CONFLICT (email) DO UPDATE
         SET senha=$3, perfil=$4, status='active', "condominioId"=$5,
             "responsavelFinanceiro"=$7, cpf=$8, "updatedAt"=$9
       RETURNING id`,
      [u.nome, u.email, hash, u.perfil, u.condominioId, u.unidadeId,
       u.responsavelFinanceiro, u.cpf, agora],
    );
    ids[u.email] = rows[0].id;
    log('  ✓', `${u.perfil.padEnd(14)} ${u.email}  (id=${rows[0].id})`);
  }

  await pool.end();
  return ids;
}

/* ─── 2. Login e token ─────────────────────────────────────────────────── */

async function login(email) {
  const r = await api(`${AUTH_API}/api/auth/login`, {
    method: 'POST', body: { email, senha: SENHA },
  });
  if (!r.token) throw new Error(`Login falhou para ${email}: ${JSON.stringify(r)}`);
  return r.token;
}

/* ─── 3. Condomínio diretamente no banco ───────────────────────────────── */

async function criarCondominio(userIds) {
  log('🏢', 'Criando condomínio no banco...');
  const pool = new pg.Pool(AUTH_DB);
  const agora = new Date().toISOString();
  await pool.query(
    `INSERT INTO condominios (id, nome, cnpj, endereco, telefone, email, status, "criadoPorId", "createdAt", "updatedAt")
     VALUES ($1,$2,$3,$4,$5,$6,'active',$7,$8,$8)
     ON CONFLICT (id) DO UPDATE SET nome=$2, "updatedAt"=$8`,
    [COND_ID, COND_NOME, '12.345.678/0001-99', 'Rua das Flores, 100, São Paulo/SP',
     '(11) 99999-0000', 'contato@parqueverde.test', userIds['admin@mora.test'], agora],
  );
  await pool.end();
  log('  ✓', `"${COND_NOME}" (${COND_ID})`);
}

/* ─── 4. Bloco + Apartamentos via portaria-service ─────────────────────── */

async function criarEstrutura(tokenSindico) {
  log('🏗 ', 'Criando bloco e apartamentos...');

  // Bloco A — endpoint correto: POST /blocos/cadastrar
  let blocoId;
  const blocoR = await api(`${PORTARIA_API}/blocos/cadastrar`, {
    method: 'POST',
    headers: bearer(tokenSindico),
    body: {
      nome: 'Bloco A',
      descricao: 'Bloco principal do condomínio',
      condominioId: COND_ID,
      andares: 2,
      apartamentosPorAndar: 2,
    },
    ignorarErro: true,
  });

  if (blocoR?.id) {
    blocoId = blocoR.id;
    log('  ✓', `Bloco A criado (${blocoId})`);
  } else {
    // Buscar bloco existente
    const lista = await api(`${PORTARIA_API}/blocos?condominioId=${COND_ID}`, {
      headers: bearer(tokenSindico), ignorarErro: true,
    });
    const blocos = Array.isArray(lista) ? lista : (lista.blocos || lista.data || []);
    const existente = blocos.find(b => b.nome === 'Bloco A');
    if (existente) {
      blocoId = existente.id;
      log('  ↩', `Bloco A já existe (${blocoId})`);
    } else {
      throw new Error(`Bloco A: ${JSON.stringify(blocoR)}`);
    }
  }

  // Apartamentos — endpoint correto: POST /apartamentos/cadastrar
  const aptos = [
    { numero: '101', andar: 1 },
    { numero: '102', andar: 1 },
    { numero: '201', andar: 2 },
    { numero: '202', andar: 2 },
  ];

  const ids = {};
  for (const a of aptos) {
    const r = await api(`${PORTARIA_API}/apartamentos/cadastrar`, {
      method: 'POST',
      headers: bearer(tokenSindico),
      body: { numero: a.numero, andar: a.andar, blocoId, quartos: 2, areaMxComTotal: 65.0 },
      ignorarErro: true,
    });
    if (r?.id) {
      ids[a.numero] = r.id;
      log('  ✓', `Apto ${a.numero} (${r.id})`);
    } else {
      // Buscar existente
      const lista = await api(`${PORTARIA_API}/apartamentos?condominioId=${COND_ID}`, {
        headers: bearer(tokenSindico), ignorarErro: true,
      });
      const all = Array.isArray(lista) ? lista : (lista.apartamentos || lista.data || []);
      const ex = all.find(x => x.numero === a.numero);
      if (ex) { ids[a.numero] = ex.id; log('  ↩', `Apto ${a.numero} já existe (${ex.id})`); }
      else log('  ⚠', `Apto ${a.numero} não criado: ${JSON.stringify(r)}`);
    }
  }

  return ids;
}

/* ─── 5. Vincular unidades aos moradores ───────────────────────────────── */

async function vincularUnidades(unidadeIds, userIds) {
  if (Object.keys(unidadeIds).length === 0) return;
  log('🔗', 'Vinculando moradores às unidades...');
  const pool = new pg.Pool(AUTH_DB);
  const agora = new Date().toISOString();

  const vinculos = [
    { email: 'morador@mora.test', apto: '101' },
    { email: 'dono@mora.test',    apto: '202' },
  ];

  for (const v of vinculos) {
    if (!unidadeIds[v.apto] || !userIds[v.email]) continue;
    await pool.query(
      `UPDATE users SET "unidadeId"=$1, "updatedAt"=$2 WHERE id=$3`,
      [unidadeIds[v.apto], agora, userIds[v.email]],
    );
    log('  ✓', `${v.email} → Apto ${v.apto} (${unidadeIds[v.apto]})`);
  }

  await pool.end();
}

/* ─── 6. Configurações financeiras ─────────────────────────────────────── */

async function configurarFinanceiro(tokenSindico, unidadeIds) {
  log('💰', 'Configurando financeiro...');

  // Regras de taxa
  await api(`${FINANCEIRO_API}/api/financeiro/regras?condominioId=${COND_ID}`, {
    method: 'PUT',
    headers: bearer(tokenSindico),
    body: { modo: 'FRACAO_IDEAL', diaFechamento: 25, diaVencimento: 10, diasRecursoMulta: 15, incluirTaxaPlataforma: true },
    ignorarErro: true,
  });
  log('  ✓', 'Regras: FRACAO_IDEAL, fechamento dia 25, vencimento dia 10');

  // Frações ideais (250 milésimos cada → 4 unidades = 1000)
  for (const uid of Object.values(unidadeIds)) {
    await api(`${FINANCEIRO_API}/api/financeiro/fracoes?condominioId=${COND_ID}`, {
      method: 'POST',
      headers: bearer(tokenSindico),
      body: { unidadeId: uid, fracaoMilesimos: 250 },
      ignorarErro: true,
    });
  }
  if (Object.keys(unidadeIds).length > 0) log('  ✓', `Frações: 250‰ × ${Object.keys(unidadeIds).length} unidades`);
  else log('  ⚠', 'Frações não configuradas (sem unidades)');

  // Taxa condominial mensal
  await api(`${FINANCEIRO_API}/api/financeiro/taxas?condominioId=${COND_ID}`, {
    method: 'POST',
    headers: bearer(tokenSindico),
    body: { nome: 'Taxa Condominial', valor: 35000, baseCalculo: 'POR_UNIDADE', periodicidade: 'MENSAL', extraordinaria: false, parcelas: 1 },
    ignorarErro: true,
  });
  log('  ✓', 'Taxa Condominial: R$ 350,00/unidade/mês');

  // Fundo de reserva mensal
  await api(`${FINANCEIRO_API}/api/financeiro/taxas?condominioId=${COND_ID}`, {
    method: 'POST',
    headers: bearer(tokenSindico),
    body: { nome: 'Fundo de Reserva', valor: 5000, baseCalculo: 'POR_UNIDADE', periodicidade: 'MENSAL', extraordinaria: false, parcelas: 1 },
    ignorarErro: true,
  });
  log('  ✓', 'Fundo de Reserva: R$ 50,00/unidade/mês');
}

/* ─── main ─────────────────────────────────────────────────────────────── */

async function main() {
  console.log('\n══════════════════════════════════════════════');
  console.log('   MORA — Seed de desenvolvimento');
  console.log('══════════════════════════════════════════════\n');

  try {
    const userIds = await criarUsuarios();
    console.log('');
    await criarCondominio(userIds);
    console.log('');

    const tokenSindico = await login('sindico@mora.test');
    log('🔐', 'Login como ADMIN_SINDICO OK');
    console.log('');

    let unidadeIds = {};
    try {
      unidadeIds = await criarEstrutura(tokenSindico);
    } catch (e) {
      log('⚠ ', `Portaria-service: ${e.message}`);
      log('  ', 'Rode novamente quando o portaria-service estiver UP');
    }

    console.log('');
    await vincularUnidades(unidadeIds, userIds);
    console.log('');

    try {
      await configurarFinanceiro(tokenSindico, unidadeIds);
    } catch (e) {
      log('⚠ ', `Financeiro: ${e.message}`);
    }

    console.log('\n══════════════════════════════════════════════');
    console.log('✅  Seed concluído!\n');
    console.log('   Senha de todos: Mora@2024');
    console.log('   ┌──────────────────────────────────────────┐');
    console.log('   │ admin@mora.test     → ADMIN_GERAL        │');
    console.log('   │ sindico@mora.test   → ADMIN_SINDICO      │');
    console.log('   │ porteiro@mora.test  → PORTEIRO           │');
    console.log('   │ morador@mora.test   → MORADOR (Apto 101) │');
    console.log('   │ dono@mora.test      → DONO_ALUGUEL (202) │');
    console.log('   └──────────────────────────────────────────┘');
    console.log('   Frontend: http://localhost:5173');
    console.log('══════════════════════════════════════════════\n');
  } catch (err) {
    console.error('\n❌  Erro:', err.message);
    process.exit(1);
  }
}

main();
