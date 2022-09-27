CREATE TABLE IF NOT EXISTS task (
    id            BIGSERIAL PRIMARY KEY,
    payload       VARCHAR                           NOT NULL,
    status        VARCHAR(20)  DEFAULT 'UBEHANDLET' NOT NULL,
    versjon       BIGINT       DEFAULT 0,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    type          VARCHAR                           NOT NULL,
    metadata      VARCHAR,
    trigger_tid   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    avvikstype    VARCHAR
);

CREATE UNIQUE INDEX IF NOT EXISTS task_payload_type_idx
    ON task (payload, type);

CREATE INDEX IF NOT EXISTS task_status_idx
    ON task (status);

CREATE TABLE IF NOT EXISTS task_logg (
    id            BIGSERIAL PRIMARY KEY,
    task_id       BIGINT       NOT NULL REFERENCES task (id),
    type          VARCHAR      NOT NULL,
    node          VARCHAR(100) NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    melding       VARCHAR,
    endret_av     VARCHAR(100) DEFAULT 'VL'
);

CREATE INDEX IF NOT EXISTS henvendelse_logg_henvendelse_id_idx
    ON task_logg (task_id);

CREATE TABLE IF NOT EXISTS fagsak_person (
    id            UUID                                NOT NULL PRIMARY KEY,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS person_ident (
    ident            VARCHAR                             NOT NULL PRIMARY KEY,
    fagsak_person_id UUID                                NOT NULL REFERENCES fagsak_person (id),
    opprettet_av     VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av        VARCHAR                             NOT NULL,
    endret_tid       TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS person_ident_fagsak_person_id_idx
    ON person_ident (fagsak_person_id);

CREATE TABLE IF NOT EXISTS fagsak (
    id               UUID                                NOT NULL PRIMARY KEY,
    stonadstype      VARCHAR                             NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av        VARCHAR                             NOT NULL,
    endret_tid       TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL,
    ekstern_id       VARCHAR                             NOT NULL,
    fagsystem        VARCHAR                             NOT NULL,
    fagsak_person_id UUID REFERENCES fagsak_person (id),
    CONSTRAINT fagsak_person_unique
        UNIQUE (fagsak_person_id, stonadstype, fagsystem)
);

CREATE INDEX IF NOT EXISTS fagsak_fagsak_person_id_idx
    ON fagsak (fagsak_person_id);

CREATE TABLE IF NOT EXISTS behandling (
    id                              UUID                                NOT NULL PRIMARY KEY,
    fagsak_id                       UUID                                NOT NULL REFERENCES fagsak (id),
    opprettet_av                    VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                   TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av                       VARCHAR                             NOT NULL,
    endret_tid                      TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL,
    status                          VARCHAR                             NOT NULL,
    steg                            VARCHAR                             NOT NULL,
    resultat                        VARCHAR                             NOT NULL,
    vedtak_dato                     TIMESTAMP    DEFAULT LOCALTIMESTAMP,
    ekstern_fagsystem_behandling_id VARCHAR                             NOT NULL,
    klage_mottatt                   TIMESTAMP(3)                        NOT NULL,
    behandlende_enhet               VARCHAR                             NOT NULL,
    ekstern_behandling_id           UUID                                NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS behandling_ekstern_behandling_id_idx
    ON behandling (ekstern_behandling_id);

CREATE TABLE IF NOT EXISTS form (
    behandling_id             UUID                                NOT NULL PRIMARY KEY REFERENCES behandling (id),
    opprettet_av              VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid             TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av                 VARCHAR                             NOT NULL,
    endret_tid                TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL,
    klage_part                VARCHAR                             NOT NULL,
    klage_konkret             VARCHAR                             NOT NULL,
    klage_signert             VARCHAR                             NOT NULL,
    klagefrist_overholdt      VARCHAR                             NOT NULL,
    saksbehandler_begrunnelse VARCHAR                             NOT NULL
);

CREATE TABLE IF NOT EXISTS vurdering (
    behandling_id UUID                                NOT NULL PRIMARY KEY REFERENCES behandling (id),
    vedtak        VARCHAR                             NOT NULL,
    arsak         VARCHAR,
    hjemmel       VARCHAR,
    beskrivelse   VARCHAR                             NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av     VARCHAR                             NOT NULL,
    endret_tid    TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS behandlingshistorikk (
    id            UUID                             NOT NULL PRIMARY KEY,
    behandling_id UUID                             NOT NULL REFERENCES behandling (id),
    steg          VARCHAR                          NOT NULL,
    opprettet_av  VARCHAR                          NOT NULL,
    endret_tid    TIMESTAMP DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS brev (
    behandling_id      UUID                                NOT NULL PRIMARY KEY REFERENCES behandling (id),
    overskrift         VARCHAR                             NOT NULL,
    saksbehandler_html VARCHAR                             NOT NULL,
    brevtype           VARCHAR                             NOT NULL,
    opprettet_av       VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av          VARCHAR                             NOT NULL,
    endret_tid         TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS avsnitt (
    avsnitt_id                UUID                                NOT NULL PRIMARY KEY,
    behandling_id             UUID                                NOT NULL REFERENCES behandling (id),
    deloverskrift             VARCHAR,
    innhold                   VARCHAR,
    skal_skjules_i_brevbygger BOOLEAN,
    opprettet_av              VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid             TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av                 VARCHAR                             NOT NULL,
    endret_tid                TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS distribusjon_resultat (
    behandling_id                 UUID                                NOT NULL PRIMARY KEY REFERENCES behandling (id),
    journalpost_id                VARCHAR,
    brev_distribusjon_id          VARCHAR,
    oversendt_til_kabal_tidspunkt TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    opprettet_av                  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                 TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av                     VARCHAR                             NOT NULL,
    endret_tid                    TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS klageresultat (
    event_id                          UUID         NOT NULL PRIMARY KEY,
    type                              VARCHAR      NOT NULL,
    utfall                            VARCHAR,
    mottatt_eller_avsluttet_tidspunkt TIMESTAMP(3) NOT NULL,
    kildereferanse                    UUID         NOT NULL,
    journalpost_referanser            VARCHAR,
    behandling_id                     UUID         NOT NULL REFERENCES behandling (id)
);

CREATE INDEX IF NOT EXISTS klageresultat_behandling_id_idx
    ON klageresultat (behandling_id);

DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = ‘cloudsqliamuser’)
        THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO cloudsqliamuser;
        END IF;
    END
$$;