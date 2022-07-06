CREATE TABLE personopplysninger (
       person_ident         VARCHAR PRIMARY KEY,
       navn                 VARCHAR NOT NULL,
       kjønn                VARCHAR NOT NULL,
       telefonnummer        VARCHAR,
       adresse              VARCHAR NOT NULL
);