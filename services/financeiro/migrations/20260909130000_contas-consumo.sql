-- Up Migration

-- Contas de consumo (água, luz, gás, internet) lançadas pelo síndico para
-- rateio entre unidades. Diferem dos tipos_taxa porque variam a cada mês
-- (valor e vencimento mudam com a fatura do fornecedor) e não são recorrentes
-- com valor fixo configurado antecipadamente.

ALTER TABLE regras_taxa
  ADD COLUMN IF NOT EXISTS incluir_taxa_plataforma BOOLEAN NOT NULL DEFAULT TRUE;

-- Tipo CONTA_CONSUMO e TAXA_PLATAFORMA para fatura_itens.
-- O CHECK original só conhecia os tipos da primeira migração; ampliar aqui
-- evita recriar a tabela que já tem dados.
ALTER TABLE fatura_itens
  DROP CONSTRAINT IF EXISTS fatura_itens_tipo_check;
ALTER TABLE fatura_itens
  ADD CONSTRAINT fatura_itens_tipo_check
  CHECK (tipo IN ('TAXA', 'MULTA', 'RESERVA', 'AVULSO', 'ESTORNO', 'CONTA_CONSUMO', 'TAXA_PLATAFORMA'));

CREATE TABLE contas_consumo (
  id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  condominio_id        VARCHAR(50) NOT NULL,
  tipo                 VARCHAR(30) NOT NULL
                       CHECK (tipo IN ('AGUA', 'LUZ', 'GAS', 'INTERNET', 'OUTRO')),
  descricao            VARCHAR(200),
  -- Sempre o dia 1 do mês de referência.
  competencia          DATE        NOT NULL CHECK (EXTRACT(DAY FROM competencia) = 1),
  valor_total_centavos BIGINT      NOT NULL CHECK (valor_total_centavos > 0),
  modo_rateio          VARCHAR(20) NOT NULL DEFAULT 'FRACAO_IDEAL'
                       CHECK (modo_rateio IN ('FRACAO_IDEAL', 'FIXO_POR_UNIDADE')),
  vencimento           DATE        NOT NULL,
  status               VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
                       CHECK (status IN ('PENDENTE', 'RATEADA', 'CANCELADA')),
  comprovante_url      TEXT,
  criado_por_id        INTEGER,
  criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contas_consumo_condominio ON contas_consumo (condominio_id, competencia)
  WHERE status <> 'CANCELADA';

-- Down Migration
ALTER TABLE regras_taxa DROP COLUMN IF EXISTS incluir_taxa_plataforma;
ALTER TABLE fatura_itens DROP CONSTRAINT IF EXISTS fatura_itens_tipo_check;
ALTER TABLE fatura_itens ADD CONSTRAINT fatura_itens_tipo_check
  CHECK (tipo IN ('TAXA', 'MULTA', 'RESERVA', 'AVULSO', 'ESTORNO'));
DROP TABLE IF EXISTS contas_consumo;
