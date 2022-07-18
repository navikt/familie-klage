CREATE TABLE brev (
    behandling_id               UUID PRIMARY KEY REFERENCES behandling(id),
    overskrift                  VARCHAR      NOT NULL,
    saksbehandler_html          VARCHAR      NOT NULL,
    brevtype                    VARCHAR      NOT NULL,

    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);