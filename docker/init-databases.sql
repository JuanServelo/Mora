-- Cria bancos adicionais para os microservicos
-- O banco 'mora' (portaria) e criado automaticamente pela variavel POSTGRES_DB
CREATE DATABASE auth_db;
CREATE DATABASE mora_meeting;
CREATE DATABASE vagas_db;
CREATE DATABASE mora_plan;

-- Conectar ao banco auth_db para criar as tabelas
\c auth_db;

-- Criar tabela condominios (clientes)
CREATE TABLE IF NOT EXISTS condominios (
  id VARCHAR(50) PRIMARY KEY,
  nome VARCHAR(200) NOT NULL,
  cnpj VARCHAR(18),
  endereco VARCHAR(300),
  telefone VARCHAR(20),
  email VARCHAR(150),
  status VARCHAR(20) DEFAULT 'active',
  "criadoPorId" INTEGER,
  "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inserir condomínio padrão
INSERT INTO condominios (id, nome, status) VALUES ('default', 'Condomínio Padrão', 'active')
ON CONFLICT (id) DO NOTHING;

-- Criar tabela users
CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  nome VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  senha VARCHAR(255),
  "googleId" VARCHAR(255) UNIQUE,
  provider VARCHAR(20) DEFAULT 'local' CHECK (provider IN ('local', 'google')),
  role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin')),
  bloco VARCHAR(50),
  apartamento VARCHAR(50),
  vaga VARCHAR(50),
  "resetToken" VARCHAR(255),
  "resetTokenExpira" TIMESTAMP,
  telefone VARCHAR(20),
  cpf VARCHAR(14),
  "fotoUrl" VARCHAR(500),
  perfil VARCHAR(50),
  status VARCHAR(30) DEFAULT 'pending_activation',
  "condominioId" VARCHAR(50),
  "unidadeId" UUID,
  "cadastradoPorId" INTEGER,
  "responsavelFinanceiro" BOOLEAN DEFAULT false,
  "dataNascimento" DATE,
  "semAcessoSistema" BOOLEAN DEFAULT false,
  "entradaPermitida" BOOLEAN DEFAULT false,
  "oauthCode" VARCHAR(255),
  "oauthCodeExpira" TIMESTAMP,
  "tokenVersion" INTEGER DEFAULT 0,
  "activatedAt" TIMESTAMP,
  "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invites (
  id SERIAL PRIMARY KEY,
  codigo VARCHAR(12) NOT NULL UNIQUE,
  email VARCHAR(150) NOT NULL,
  perfil VARCHAR(50) NOT NULL,
  "condominioId" VARCHAR(50) NOT NULL,
  "unidadeId" UUID,
  "nomePrecadastro" VARCHAR(150),
  "cpfPrecadastro" VARCHAR(14),
  "cadastradoPorId" INTEGER NOT NULL,
  status VARCHAR(20) DEFAULT 'pending',
  "expiresAt" TIMESTAMP NOT NULL,
  "usedAt" TIMESTAMP,
  "usedByUserId" INTEGER,
  "responsavelFinanceiro" BOOLEAN NOT NULL DEFAULT false,
  "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Portaria: histórico de entradas e saídas (um registro por evento ENTRADA/SAIDA)
CREATE TABLE IF NOT EXISTS registros_acesso (
  id                SERIAL PRIMARY KEY,
  "usuarioId"       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  tipo              VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
  "registradoPorId" INTEGER REFERENCES users(id) ON DELETE SET NULL,
  "condominioId"    VARCHAR(50),
  "nomeSnapshot"    VARCHAR(150),
  "perfilSnapshot"  VARCHAR(50),
  "createdAt"       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_registros_acesso_usuario
  ON registros_acesso ("usuarioId", "createdAt" DESC);
CREATE INDEX IF NOT EXISTS idx_registros_acesso_condominio
  ON registros_acesso ("condominioId", "createdAt" DESC);

-- Usuários de teste por perfil (senha: Teste@1234)
-- ADMIN_GERAL   → admin@mora.com        senha: Teste@1234

INSERT INTO users (nome, email, senha, perfil, role, status, "condominioId", "tokenVersion", "activatedAt", "createdAt", "updatedAt")
VALUES
  ('Administrador Geral',  'admin@mora.com',    '$2a$10$et7p4t932Oh1VUoAuMw6VewAoqiDNro3Ka2clNo8VEVFCVJbatGOe', 'ADMIN_GERAL',    'admin', 'active', 'default', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Criar índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_condominios_status ON condominios(status);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_googleId ON users("googleId");
CREATE INDEX IF NOT EXISTS idx_users_condominioId ON users("condominioId");
CREATE INDEX IF NOT EXISTS idx_users_unidadeId ON users("unidadeId");
CREATE INDEX IF NOT EXISTS idx_invites_condominioId ON invites("condominioId");

-- Conectar ao banco mora (portaria) para criar as tabelas de estrutura física
\c mora;

-- Criar tabela blocos
CREATE TABLE IF NOT EXISTS blocos (
  id UUID PRIMARY KEY,
  nome VARCHAR(255) NOT NULL,
  "condominioId" VARCHAR(50),
  descricao TEXT,
  andares INTEGER,
  "apartamentosPorAndar" INTEGER,
  ativo BOOLEAN DEFAULT true,
  "criadoEm" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  "atualizadoEm" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(nome, "condominioId")
);

-- Criar tabela apartamentos
CREATE TABLE IF NOT EXISTS apartamentos (
  id UUID PRIMARY KEY,
  numero VARCHAR(50) NOT NULL,
  andar INTEGER NOT NULL,
  "blocoId" UUID NOT NULL REFERENCES blocos(id),
  quartos INTEGER,
  "areaMxComTotal" DOUBLE PRECISION,
  observacoes TEXT,
  ativo BOOLEAN DEFAULT true,
  "criadoEm" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  "atualizadoEm" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE("numero", "blocoId")
);

-- Criar tabela areas_comuns
CREATE TABLE IF NOT EXISTS areas_comuns (
  id UUID PRIMARY KEY,
  nome VARCHAR(255) NOT NULL,
  "condominioId" VARCHAR(50),
  tipo VARCHAR(100) NOT NULL,
  descricao TEXT,
  localizacao VARCHAR(255),
  "capacidadeMaxima" INTEGER,
  area DOUBLE PRECISION,
  "podeReservar" BOOLEAN DEFAULT false,
  observacoes TEXT,
  ativo BOOLEAN DEFAULT true,
  "criadoEm" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  "atualizadoEm" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(nome, "condominioId")
);

-- Criar índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_blocos_ativo ON blocos(ativo);
CREATE INDEX IF NOT EXISTS idx_apartamentos_blocoId ON apartamentos("blocoId");
CREATE INDEX IF NOT EXISTS idx_apartamentos_ativo ON apartamentos(ativo);
CREATE INDEX IF NOT EXISTS idx_apartamentos_numero_blocoId ON apartamentos("numero", "blocoId");
CREATE INDEX IF NOT EXISTS idx_areas_comuns_tipo ON areas_comuns(tipo);
CREATE INDEX IF NOT EXISTS idx_areas_comuns_ativo ON areas_comuns(ativo);
CREATE INDEX IF NOT EXISTS idx_areas_comuns_pode_reservar ON areas_comuns("podeReservar");
