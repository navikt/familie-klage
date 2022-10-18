ALTER TABLE behandling ADD COLUMN paklaget_vedtak varchar NOT NULL DEFAULT 'Vedtak'; -- Kun aktuelt for testdata
ALTER TABLE behandling ALTER COLUMN ekstern_fagsystem_behandling_id DROP NOT NULL;