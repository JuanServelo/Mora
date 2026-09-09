-- =============================================================================
-- FASE 4: Merge carros → veiculos
-- Objetivo: consolidar toda a gestão de veículos na tabela 'veiculos'
-- Executar no banco 'mora' ANTES de remover o CarroController
-- =============================================================================

-- 1. Copiar registros de 'carros' que ainda não existem em 'veiculos' (pela placa)
INSERT INTO veiculos (id, placa, modelo, "condominioId", categoria,
                      proprietario_id, tipo_proprietario, vaga_id,
                      status, data_entrada, data_saida,
                      criado_em, atualizado_em)
SELECT
    c.id,
    c.placa,
    c.modelo,
    c."condominioId",
    'CARRO'                  AS categoria,
    c.proprietario_id,
    c.tipo_proprietario,
    NULL                     AS vaga_id,
    COALESCE(c.status::text, 'SAIU') AS status,
    c.data_entrada,
    c.data_saida,
    c.criado_em,
    c.atualizado_em
FROM carros c
WHERE NOT EXISTS (
    SELECT 1 FROM veiculos v WHERE v.placa = c.placa
);

-- 2. Verificar contagens após migração
SELECT 'carros'   AS tabela, COUNT(*) FROM carros
UNION ALL
SELECT 'veiculos',                     COUNT(*) FROM veiculos;

-- 3. Após confirmar os dados, a tabela 'carros' pode ser mantida por segurança.
--    O CarroController será marcado como deprecated e eventualmente removido.
--    A tabela pode ser dropada em uma release futura:
--    DROP TABLE IF EXISTS carros;
