CREATE TABLE personopplysninger (
       person_id            VARCHAR PRIMARY KEY NOT NULL,
       navn                 VARCHAR NOT NULL,
       kjonn                VARCHAR NOT NULL,
       telefonnummer        VARCHAR,
       adresse              VARCHAR NOT NULL
);