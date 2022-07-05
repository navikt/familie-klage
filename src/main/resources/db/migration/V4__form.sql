CREATE TABLE form (
    behandling_id               UUID PRIMARY KEY,
    fagsak_id                   VARCHAR      NOT NULL,

    vedtaksdato                 TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    klage_mottat                TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    klageaarsak                 VARCHAR      NOT NULL,

    klage_beskrivelse           VARCHAR      NOT NULL,
    klage_part                  VARCHAR      DEFAULT 'IKKE_SATT',
    klage_konkret               VARCHAR      DEFAULT 'IKKE_SATT',
    klage_signert               VARCHAR      DEFAULT 'IKKE_SATT',
    klagefrist_overholdt        VARCHAR      DEFAULT 'IKKE_SATT',
    saksbehandler_begrunnelse   VARCHAR      DEFAULT 'Begrunnelse kommer her',
    sak_sist_endret             TIMESTAMP    DEFAULT LOCALTIMESTAMP
);