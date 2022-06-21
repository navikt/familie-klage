CREATE TABLE behandlingshistorikk (
    id                UUID PRIMARY KEY,
    behandling_id     UUID REFERENCES behandling (id),

    steg              VARCHAR   NOT NULL,
    opprettet_av_navn VARCHAR   NOT NULL,
    opprettet_av      VARCHAR   NOT NULL,
    utfall            VARCHAR,

    endret_tid        TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP
)