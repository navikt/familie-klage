CREATE TABLE form (
    behandling_id               UUID PRIMARY KEY,
    fagsak_id                   VARCHAR      NOT NULL,

    klage_part                  VARCHAR      DEFAULT 'IKKE_SATT',
    klage_konkret               VARCHAR      DEFAULT 'IKKE_SATT',
    klage_signert               VARCHAR      DEFAULT 'IKKE_SATT',
    klagefrist_overholdt        VARCHAR      DEFAULT 'IKKE_SATT',
    saksbehandler_begrunnelse   VARCHAR      DEFAULT 'IKKE_SATT'
);