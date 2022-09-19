CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE behandling
    RENAME ekstern_behandling_id TO ekstern_fagsystem_behandling_id;

ALTER TABLE behandling
    ADD COLUMN ekstern_behandling_id UUID;
CREATE UNIQUE INDEX ON behandling (ekstern_behandling_id);
UPDATE behandling
SET ekstern_behandling_id = uuid_generate_v4()
WHERE ekstern_behandling_id IS NULL;

ALTER TABLE behandling
    ALTER COLUMN ekstern_behandling_id SET NOT NULL;