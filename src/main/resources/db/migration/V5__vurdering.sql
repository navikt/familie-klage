CREATE TABLE vurdering (
    behandling_id        UUID PRIMARY KEY,
    vedtak               VARCHAR NOT NULL,
    arsak                VARCHAR,
    hjemmel              VARCHAR,
    beskrivelse          VARCHAR NOT NULL,
    fullfort_dato        TIMESTAMP DEFAULT LOCALTIMESTAMP
);