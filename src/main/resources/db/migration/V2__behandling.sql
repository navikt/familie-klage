CREATE TABLE behandling (
    id                   UUID PRIMARY KEY,
    fagsak_id            VARCHAR      NOT NULL,
    behandling_id_stonad VARCHAR      NOT NULL,
    stonadstype          VARCHAR      NOT NULL,
    versjon              INT                   DEFAULT 0,

    opprettet_av         VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid        TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av            VARCHAR      NOT NULL,
    endret_tid           TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,

    type                 VARCHAR      NOT NULL,
    status               VARCHAR      NOT NULL,
    steg                 VARCHAR      NOT NULL
);