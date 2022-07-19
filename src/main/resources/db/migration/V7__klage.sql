CREATE TABLE klage (
    behandling_id               UUID PRIMARY KEY,
    fagsak_id                   VARCHAR      NOT NULL,

    vedtaks_dato                 TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    klage_mottatt                TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    klage_aarsak                 VARCHAR      NOT NULL,

    klage_beskrivelse           VARCHAR      NOT NULL,
    sak_sist_endret             TIMESTAMP    DEFAULT LOCALTIMESTAMP
);