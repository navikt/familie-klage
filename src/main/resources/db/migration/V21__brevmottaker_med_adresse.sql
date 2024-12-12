CREATE TABLE IF NOT EXISTS brevmottaker_med_adresse
(
    behandling_id  UUID                                NOT NULL PRIMARY KEY REFERENCES behandling (id),
    type           VARCHAR(50)                         NOT NULL,
    navn           VARCHAR                             NOT NULL,
    adresselinje_1 VARCHAR                             NOT NULL,
    adresselinje_2 VARCHAR,
    postnummer     VARCHAR                             NOT NULL,
    poststed       VARCHAR                             NOT NULL,
    landkode       VARCHAR(2)                          NOT NULL,
    opprettet_av   VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av      VARCHAR                             NOT NULL,
    endret_tid     TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);