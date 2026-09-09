-- =============================================================================
-- FASE 3: Migração vagas-service → portaria-service
-- Executar no banco 'mora' (com acesso ao banco 'vagas_db' via dblink ou pg_dump/restore)
-- =============================================================================

-- 1. Criar as tabelas no banco mora (o Spring fará isso automaticamente via ddl-auto:update
--    ao iniciar portaria-service com os novos modelos. Rodar portaria primeiro.)

-- 2. Copiar disponibilidade_vagas de vagas_db → mora
-- Necessário que o usuário DB tenha permissão de SELECT no vagas_db, ou usar pg_dump:
--
--   Na máquina host:
--   pg_dump -U admin -d vagas_db -t disponibilidade_vagas --data-only | psql -U admin -d mora
--   pg_dump -U admin -d vagas_db -t alugueis_vagas       --data-only | psql -U admin -d mora

-- 3. Verificar integridade referencial após cópia:
--    As FKs de disponibilidade_vagas.vaga_id e alugueis_vagas.vaga_id
--    referenciam vagas_estacionamento em mora. Como vagas_estacionamento
--    foi sincronizado pelo portaria-service, os IDs já existem.

-- 4. Verificar contagens
SELECT 'disponibilidade_vagas' AS tabela, COUNT(*) FROM disponibilidade_vagas
UNION ALL
SELECT 'alugueis_vagas',                  COUNT(*) FROM alugueis_vagas;

-- 5. Após confirmar os dados, desativar o vagas-service:
--    - Remover ou comentar o bloco 'vagas-service' do docker-compose.yml
--    - O banco vagas_db pode ser mantido por segurança e dropado depois
