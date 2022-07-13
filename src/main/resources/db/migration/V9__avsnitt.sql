CREATE TABLE avsnitt (
    avsnitt_id                  UUID PRIMARY KEY,
    brev_id                     UUID REFERENCES brev(brev_id),
    deloverskrift               VARCHAR,
    innhold                     VARCHAR,
    skal_skjules_i_brevbygger   BOOLEAN
);