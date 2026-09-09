-- =============================================================================
-- FASE 5: Tabelas novas do comunicacao-service
-- Criadas automaticamente pelo Spring ddl-auto=update ao subir o serviço.
-- Avisos e artigos_conhecimento já existem no banco mora (criados pelo portaria-service).
-- =============================================================================

-- Rastreamento de leitura de avisos (RF: aviso_leitura)
CREATE TABLE IF NOT EXISTS aviso_leituras (
    id          VARCHAR(36)  PRIMARY KEY,
    aviso_id    UUID         NOT NULL,
    usuario_id  UUID         NOT NULL,
    lido_em     TIMESTAMP    DEFAULT NOW(),
    UNIQUE (aviso_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_aviso_leituras_aviso   ON aviso_leituras (aviso_id);
CREATE INDEX IF NOT EXISTS idx_aviso_leituras_usuario ON aviso_leituras (usuario_id);

-- Notificações internas
CREATE TABLE IF NOT EXISTS notificacoes (
    id              VARCHAR(36)  PRIMARY KEY,
    destinatario_id UUID         NOT NULL,
    "condominioId"  VARCHAR(255),
    tipo            VARCHAR(30),
    titulo          VARCHAR(255) NOT NULL,
    mensagem        TEXT,
    referencia_id   VARCHAR(36),
    lida            BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP    DEFAULT NOW(),
    lida_em         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_destinatario ON notificacoes (destinatario_id);
CREATE INDEX IF NOT EXISTS idx_notificacoes_lida         ON notificacoes (destinatario_id, lida);

-- Mensagens de chat entre usuários
CREATE TABLE IF NOT EXISTS chat_mensagens (
    id              VARCHAR(36)  PRIMARY KEY,
    remetente_id    UUID         NOT NULL,
    destinatario_id UUID,
    "condominioId"  VARCHAR(255),
    texto           TEXT         NOT NULL,
    lida            BOOLEAN      NOT NULL DEFAULT FALSE,
    enviado_em      TIMESTAMP    DEFAULT NOW(),
    lida_em         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_remetente    ON chat_mensagens (remetente_id);
CREATE INDEX IF NOT EXISTS idx_chat_destinatario ON chat_mensagens (destinatario_id);

-- =============================================================================
-- Migração de dados: após o comunicacao-service se tornar o owner de avisos/artigos,
-- marcar endpoints do portaria-service como deprecated e redirecionar tráfego via Traefik.
-- As tabelas 'avisos' e 'artigos_conhecimento' não precisam de migração pois
-- ambos os serviços leem do mesmo banco mora.
-- =============================================================================
