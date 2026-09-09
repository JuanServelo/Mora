-- Up Migration

CREATE TABLE notificacoes (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id     INTEGER     NOT NULL,
  condominio_id  VARCHAR(50),
  tipo           VARCHAR(50) NOT NULL
                 CHECK (tipo IN ('NOVA_FATURA', 'PAGAMENTO_CONFIRMADO', 'FATURA_VENCIDA', 'SISTEMA')),
  titulo         VARCHAR(200) NOT NULL,
  mensagem       TEXT,
  lida           BOOLEAN     NOT NULL DEFAULT FALSE,
  metadados      JSONB,
  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sustenta a listagem de alertas não lidos que aparece no badge da navbar.
CREATE INDEX idx_notificacoes_usuario_nao_lida ON notificacoes (usuario_id, criado_em DESC)
  WHERE NOT lida;
CREATE INDEX idx_notificacoes_usuario ON notificacoes (usuario_id, criado_em DESC);

-- Down Migration
DROP TABLE IF EXISTS notificacoes;
