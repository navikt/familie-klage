ALTER TABLE vurdering
    ADD COLUMN dokumentasjon_og_utredning VARCHAR,
    ADD COLUMN sporsmalet_i_saken         VARCHAR,
    ADD COLUMN aktuelle_rettskilder       VARCHAR,
    ADD COLUMN klagers_anforsler          VARCHAR,
    ADD COLUMN vurdering_av_klagen        VARCHAR;