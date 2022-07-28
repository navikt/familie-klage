CREATE TABLE fagsak (
    id                  UUID         PRIMARY KEY,
    person_ident        VARCHAR      NOT NULL REFERENCES personopplysninger(person_ident),
    stonadstype         VARCHAR      NOT NULL,
    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT localtimestamp
);
