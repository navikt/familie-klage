CREATE TABLE vurdering (
    behandling_id        UUID PRIMARY KEY,
    vedtak               VARCHAR NOT NULL,
    arsak                VARCHAR,
    hjemmel              VARCHAR,
    beskrivelse          VARCHAR NOT NULL,

    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);