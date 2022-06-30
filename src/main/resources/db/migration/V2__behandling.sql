CREATE TABLE behandling (
    id                   UUID PRIMARY KEY,
    fagsak_id            VARCHAR      NOT NULL,

    opprettet_tid        TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_tid           TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,

    status               VARCHAR      NOT NULL,
    steg                 VARCHAR      NOT NULL,
    fagsystem            VARCHAR      NOT NULL,
    resultat             VARCHAR      NOT NULL,
    vedtak_dato          TIMESTAMP    DEFAULT LOCALTIMESTAMP
);