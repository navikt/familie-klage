ALTER TABLE form
    ADD CONSTRAINT form_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id);
ALTER TABLE form
    ALTER COLUMN klage_part DROP DEFAULT;
ALTER TABLE form
    ALTER COLUMN klage_konkret DROP DEFAULT;
ALTER TABLE form
    ALTER COLUMN klage_signert DROP DEFAULT;
ALTER TABLE form
    ALTER COLUMN klagefrist_overholdt DROP DEFAULT;
ALTER TABLE form
    ALTER COLUMN saksbehandler_begrunnelse DROP DEFAULT;

ALTER TABLE form
    ALTER COLUMN klage_part SET NOT NULL;
ALTER TABLE form
    ALTER COLUMN klage_konkret SET NOT NULL;
ALTER TABLE form
    ALTER COLUMN klage_signert SET NOT NULL;
ALTER TABLE form
    ALTER COLUMN klagefrist_overholdt SET NOT NULL;
ALTER TABLE form
    ALTER COLUMN saksbehandler_begrunnelse SET NOT NULL;

ALTER TABLE vurdering
    ADD CONSTRAINT vurdering_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling (id);

DROP TABLE klage;

