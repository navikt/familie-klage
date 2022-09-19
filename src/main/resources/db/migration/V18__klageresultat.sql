CREATE TABLE klageresultat (
    event_id               UUID PRIMARY KEY,
    type                   VARCHAR      NOT NULL,
    utfall                 VARCHAR,
    hendelse_tidspunkt     TIMESTAMP(3) NOT NULL,
    kildereferanse         UUID         NOT NULL,
    journalpost_referanser VARCHAR
);
