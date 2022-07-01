CREATE TABLE form (
    id                          UUID PRIMARY KEY,
    fagsak_id                   VARCHAR      NOT NULL,

    vedtaksdato                 TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    klage_mottat                TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    klageaarsak                 VARCHAR      NOT NULL,

    klage_beskrivelse           VARCHAR      NOT NULL,
    klage_part                  VARCHAR      NOT NULL,
    klage_konkret               VARCHAR      NOT NULL,
    klage_signert               VARCHAR      NOT NULL,
    klagefrist_overholdt        VARCHAR      NOT NULL,
    saksbehandler_begrunnelse   VARCHAR      NOT NULL,
    sak_sist_endret             TIMESTAMP    DEFAULT LOCALTIMESTAMP
);