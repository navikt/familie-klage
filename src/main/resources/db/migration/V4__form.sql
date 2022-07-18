CREATE TABLE form (
    behandling_id               UUID PRIMARY KEY,
    fagsak_id                   VARCHAR      NOT NULL,

    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,

    klage_part                  VARCHAR      DEFAULT 'IKKE_SATT',
    klage_konkret               VARCHAR      DEFAULT 'IKKE_SATT',
    klage_signert               VARCHAR      DEFAULT 'IKKE_SATT',
    klagefrist_overholdt        VARCHAR      DEFAULT 'IKKE_SATT',
    saksbehandler_begrunnelse   VARCHAR      DEFAULT 'IKKE_SATT'
);