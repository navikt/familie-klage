ALTER TABLE behandlingshistorikk ADD COLUMN historikk_hendelse VARCHAR;

ALTER TABLE behandlingshistorikk DROP COLUMN IF EXISTS behandling_status;
