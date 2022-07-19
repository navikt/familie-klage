CREATE TABLE avsnitt (
    avsnitt_id                  UUID PRIMARY KEY,
    behandling_id               UUID REFERENCES behandling(id),
    deloverskrift               VARCHAR,
    innhold                     VARCHAR,
    skal_skjules_i_brevbygger   BOOLEAN,

    opprettet_av        VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av           VARCHAR      NOT NULL,
    endret_tid          TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);