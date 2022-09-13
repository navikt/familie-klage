ALTER TABLE behandling ADD COLUMN behandlende_enhet VARCHAR;
UPDATE behandling SET behandlende_enhet = '4489'; -- For å oppdatere lokaldatabse og preprod setter vi til 4489. Ingen saker i prod enda.
ALTER TABLE behandling ALTER COLUMN behandlende_enhet SET NOT NULL;