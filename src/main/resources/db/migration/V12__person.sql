CREATE TABLE fagsak_person (
    id            UUID PRIMARY KEY NOT NULL,
    opprettet_av  VARCHAR          NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3)     NOT NULL DEFAULT LOCALTIMESTAMP
);

CREATE TABLE person_ident (
    ident            VARCHAR PRIMARY KEY,
    fagsak_person_id UUID         NOT NULL REFERENCES fagsak_person (id),
    opprettet_av     VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ON person_ident (fagsak_person_id);


ALTER TABLE behandling
    DROP COLUMN stonads_type;
ALTER TABLE behandling
    DROP COLUMN fagsystem;

ALTER TABLE behandling
    ADD COLUMN ekstern_behandling_id VARCHAR NOT NULL;

ALTER TABLE fagsak
    ADD COLUMN ekstern_id VARCHAR NOT NULL;
ALTER TABLE fagsak
    ADD COLUMN fagsystem VARCHAR NOT NULL;

ALTER TABLE fagsak
    ADD COLUMN fagsak_person_id UUID REFERENCES fagsak_person (id);
CREATE INDEX ON fagsak (fagsak_person_id);
ALTER TABLE fagsak
    ADD CONSTRAINT fagsak_person_unique UNIQUE (fagsak_person_id, stonadstype, fagsystem);

