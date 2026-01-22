CREATE TABLE IF NOT EXISTS INSTITUSJON
(
    id             UUID           NOT NULL PRIMARY KEY,
    orgnummer      VARCHAR UNIQUE NOT NULL,
    navn           VARCHAR        NOT NULL,
    tss_ekstern_id VARCHAR UNIQUE NOT NULL
);

ALTER TABLE FAGSAK
    ADD COLUMN fk_institusjon_id UUID;

ALTER TABLE FAGSAK
    ADD FOREIGN KEY (fk_institusjon_id) REFERENCES INSTITUSJON (id);