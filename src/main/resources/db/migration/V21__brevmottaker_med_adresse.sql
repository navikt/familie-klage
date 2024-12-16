CREATE TABLE IF NOT EXISTS brevmottaker
(
    id             UUID                                NOT NULL PRIMARY KEY,
    behandling_id  UUID                                NOT NULL REFERENCES behandling (id),
    type           VARCHAR(50)                         NOT NULL,
    navn           VARCHAR                             NOT NULL,
    adresselinje_1 VARCHAR                             NOT NULL,
    adresselinje_2 VARCHAR,
    postnummer     VARCHAR(4),
    poststed       VARCHAR,
    landkode       VARCHAR(2)                          NOT NULL,
    opprettet_av   VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av      VARCHAR                             NOT NULL,
    endret_tid     TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);