ALTER TABLE fagsak
    RENAME COLUMN fagsak_person_id to fagsak_eier_person_id;

ALTER TABLE fagsak
    ADD COLUMN soker_person_id UUID;

ALTER TABLE fagsak
    ADD CONSTRAINT fk_fagsak_soker_person_id FOREIGN KEY (soker_person_id) REFERENCES fagsak_person (id);

UPDATE fagsak
SET soker_person_id = fagsak_eier_person_id
WHERE soker_person_id IS NULL;

ALTER TABLE fagsak
    ALTER COLUMN soker_person_id SET NOT NULL;