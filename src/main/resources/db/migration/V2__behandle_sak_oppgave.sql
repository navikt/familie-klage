CREATE TABLE behandle_sak_oppgave (
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    oppgave_id    BIGINT       NOT NULL,

    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR      NOT NULL,
    endret_tid    TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);