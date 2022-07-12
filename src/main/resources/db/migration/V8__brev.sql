CREATE TABLE brev (
    brev_id                     UUID PRIMARY KEY,
    behandling_id               UUID REFERENCES behandling(id),
    overskrift                  VARCHAR      NOT NULL
);