-- =============================================================================
-- FASE 4: Tabelas criadas automaticamente pelo Spring ddl-auto=update
-- Este script documenta as estruturas e pode ser usado para criação manual
-- =============================================================================

-- RF-10: Reservas de áreas comuns
CREATE TABLE IF NOT EXISTS reservas (
    id               VARCHAR(36) PRIMARY KEY,
    solicitante_id   UUID         NOT NULL,
    "condominioId"   VARCHAR(255),
    area_comum_id    VARCHAR(36) REFERENCES areas_comuns(id),
    data_inicio      DATE         NOT NULL,
    data_fim         DATE         NOT NULL,
    hora_inicio      TIME,
    hora_fim         TIME,
    valor_total      NUMERIC(10,2),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    motivo_recusa    VARCHAR(500),
    observacoes      VARCHAR(500),
    criado_em        TIMESTAMP    DEFAULT NOW(),
    atualizado_em    TIMESTAMP    DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reservas_condominio    ON reservas ("condominioId");
CREATE INDEX IF NOT EXISTS idx_reservas_area_comum    ON reservas (area_comum_id);
CREATE INDEX IF NOT EXISTS idx_reservas_solicitante   ON reservas (solicitante_id);
CREATE INDEX IF NOT EXISTS idx_reservas_status        ON reservas (status);

-- RF-6: Pré-autorização de visitantes
CREATE TABLE IF NOT EXISTS pre_autorizacoes (
    id               VARCHAR(36)  PRIMARY KEY,
    morador_id       UUID         NOT NULL,
    "condominioId"   VARCHAR(255),
    nome_visitante   VARCHAR(255) NOT NULL,
    cpf_visitante    VARCHAR(14),
    validade_inicio  DATE         NOT NULL,
    validade_fim     DATE         NOT NULL,
    observacoes      VARCHAR(500),
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP    DEFAULT NOW(),
    atualizado_em    TIMESTAMP    DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_preaut_condominio     ON pre_autorizacoes ("condominioId");
CREATE INDEX IF NOT EXISTS idx_preaut_morador        ON pre_autorizacoes (morador_id);
CREATE INDEX IF NOT EXISTS idx_preaut_validade       ON pre_autorizacoes (validade_inicio, validade_fim);

-- RF-7: Vínculo de entrega ao morador
ALTER TABLE entregas ADD COLUMN IF NOT EXISTS morador_id UUID;
CREATE INDEX IF NOT EXISTS idx_entregas_morador ON entregas (morador_id);
