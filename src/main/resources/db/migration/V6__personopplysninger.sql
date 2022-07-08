CREATE TABLE personopplysninger (
       behandling_id        UUID PRIMARY KEY REFERENCES behandling (id),
       person_id            VARCHAR,
       navn                 VARCHAR NOT NULL,
       kjonn                VARCHAR NOT NULL,
       telefonnummer        VARCHAR,
       adresse              VARCHAR NOT NULL
);