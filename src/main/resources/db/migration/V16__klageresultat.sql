CREATE TABLE klageresultat (
    behandling_id                 UUID PRIMARY KEY REFERENCES behandling (id),
    journalpost_id                VARCHAR,
    distribusjon_id               VARCHAR,
    oversendt_til_kabal_tidspunkt TIMESTAMP(3)          DEFAULT LOCALTIMESTAMP,

    opprettet_av                  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid                 TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av                     VARCHAR      NOT NULL,
    endret_tid                    TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);