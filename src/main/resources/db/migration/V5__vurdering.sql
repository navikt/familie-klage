CREATE TABLE vurdering (
    behandling_id         UUID PRIMARY KEY,

    oppfylt_formkrav     INT NOT NULL,
    mulig_formkrav       INT NOT NULL,
    begrunnelse          VARCHAR NOT NULL,

    vedtak_valg          VARCHAR NOT NULL,
    årsak                VARCHAR,
    hjemmel              VARCHAR,
    beskrivelse          VARCHAR NOT NULL,
    fullført_dato       TIMESTAMP DEFAULT LOCALTIMESTAMP
);