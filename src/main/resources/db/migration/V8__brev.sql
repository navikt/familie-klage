CREATE TABLE brev (
    behandling_id               UUID PRIMARY KEY REFERENCES behandling(id),
    overskrift                  VARCHAR      NOT NULL,
    saksbehandler_html          VARCHAR      NOT NULL
);