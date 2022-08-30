ALTER TABLE behandling DROP COLUMN klage_mottatt;
ALTER TABLE behandling ADD COLUMN klage_mottatt TIMESTAMP(3) NOT NULL;