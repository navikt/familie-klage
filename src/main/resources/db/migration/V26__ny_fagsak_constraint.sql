ALTER TABLE fagsak
    DROP CONSTRAINT fagsak_person_unique,
    ADD CONSTRAINT fagsak_person_unique UNIQUE (fagsak_person_id, ekstern_id, fagsystem)
