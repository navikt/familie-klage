CREATE TABLE klageresultat
(
    eventId               UUID PRIMARY KEY,
    utfall                VARCHAR,
    vedtakstidspunkt      TIMESTAMP(3) NOT NULL,
    kildereferanse        UUID         NOT NULL,
    journalpostReferanser JSON
);
