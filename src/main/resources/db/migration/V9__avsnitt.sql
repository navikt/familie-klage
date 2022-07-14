CREATE TABLE avsnitt (
    avsnitt_id                  UUID PRIMARY KEY,
    behandling_id               UUID REFERENCES behandling(id),
    deloverskrift               VARCHAR,
    innhold                     VARCHAR,
    skal_skjules_i_brevbygger   BOOLEAN
);