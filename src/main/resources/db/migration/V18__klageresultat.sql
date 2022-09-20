CREATE TABLE klageresultat (
    event_id                          UUID PRIMARY KEY,
    type                              VARCHAR      NOT NULL,
    utfall                            VARCHAR,
    mottatt_eller_avsluttet_tidspunkt TIMESTAMP(3) NOT NULL,
    kildereferanse                    UUID         NOT NULL,
    journalpost_referanser            VARCHAR,
    behandling_id                     UUID         NOT NULL REFERENCES behandling (id)
);

CREATE INDEX ON klageresultat (behandling_id);
