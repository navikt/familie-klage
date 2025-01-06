CREATE TABLE IF NOT EXISTS baks_brev
(
    behandling_id      UUID                                NOT NULL PRIMARY KEY REFERENCES behandling (id),
    overskrift         VARCHAR                             NOT NULL,
    saksbehandler_html VARCHAR                             NOT NULL,
    brevtype           VARCHAR                             NOT NULL,
    opprettet_av       VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid      TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av          VARCHAR                             NOT NULL,
    endret_tid         TIMESTAMP    DEFAULT LOCALTIMESTAMP NOT NULL
);

