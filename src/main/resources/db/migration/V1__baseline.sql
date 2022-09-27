create table if not exists task
(
    id bigserial
        constraint task_pkey
            primary key,
    payload varchar not null,
    status varchar(20) default 'UBEHANDLET'::character varying not null,
    versjon bigint default 0,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP,
    type varchar not null,
    metadata varchar,
    trigger_tid timestamp(3) default LOCALTIMESTAMP,
    avvikstype varchar
);

create unique index if not exists task_payload_type_idx
    on task (payload, type);

create index if not exists task_status_idx
    on task (status);

create table if not exists task_logg
(
    id bigserial
        constraint task_logg_pkey
            primary key,
    task_id bigint not null
        constraint henvendelse_logg_henvendelse_id_fkey
            references task,
    type varchar not null,
    node varchar(100) not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP,
    melding varchar,
    endret_av varchar(100) default 'VL'::character varying
);

create index if not exists henvendelse_logg_henvendelse_id_idx
    on task_logg (task_id);

create table if not exists form
(
    behandling_id uuid not null
        constraint form_pkey
            primary key,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null,
    klage_part varchar default 'IKKE_SATT'::character varying,
    klage_konkret varchar default 'IKKE_SATT'::character varying,
    klage_signert varchar default 'IKKE_SATT'::character varying,
    klagefrist_overholdt varchar default 'IKKE_SATT'::character varying,
    saksbehandler_begrunnelse varchar default 'IKKE_SATT'::character varying
);

create table if not exists vurdering
(
    behandling_id uuid not null
        constraint vurdering_pkey
            primary key,
    vedtak varchar not null,
    arsak varchar,
    hjemmel varchar,
    beskrivelse varchar not null,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null
);

create table if not exists klage
(
    behandling_id uuid not null
        constraint klage_pkey
            primary key,
    fagsak_id varchar not null,
    vedtaks_dato timestamp default LOCALTIMESTAMP not null,
    klage_mottatt timestamp default LOCALTIMESTAMP not null,
    klage_aarsak varchar not null,
    klage_beskrivelse varchar not null,
    sak_sist_endret timestamp default LOCALTIMESTAMP
);

create table if not exists fagsak_person
(
    id uuid not null
        constraint fagsak_person_pkey
            primary key,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null
);

create table if not exists fagsak
(
    id uuid not null
        constraint fagsak_pkey
            primary key,
    stonadstype varchar not null,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null,
    ekstern_id varchar not null,
    fagsystem varchar not null,
    fagsak_person_id uuid
        constraint fagsak_fagsak_person_id_fkey
            references fagsak_person,
    constraint fagsak_person_unique
        unique (fagsak_person_id, stonadstype, fagsystem)
);

create index if not exists fagsak_fagsak_person_id_idx
    on fagsak (fagsak_person_id);

create table if not exists behandling
(
    id uuid not null
        constraint behandling_pkey
            primary key,
    fagsak_id uuid not null
        constraint behandling_fagsak_id_fkey
            references fagsak,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null,
    status varchar not null,
    steg varchar not null,
    resultat varchar not null,
    vedtak_dato timestamp default LOCALTIMESTAMP,
    ekstern_fagsystem_behandling_id varchar not null,
    klage_mottatt timestamp(3) not null,
    behandlende_enhet varchar not null,
    ekstern_behandling_id uuid not null
);

create unique index if not exists behandling_ekstern_behandling_id_idx
    on behandling (ekstern_behandling_id);

create table if not exists behandlingshistorikk
(
    id uuid not null
        constraint behandlingshistorikk_pkey
            primary key,
    behandling_id uuid
        constraint behandlingshistorikk_behandling_id_fkey
            references behandling,
    steg varchar not null,
    opprettet_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null
);

create table if not exists brev
(
    behandling_id uuid not null
        constraint brev_pkey
            primary key
        constraint brev_behandling_id_fkey
            references behandling,
    overskrift varchar not null,
    saksbehandler_html varchar not null,
    brevtype varchar not null,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null
);

create table if not exists avsnitt
(
    avsnitt_id uuid not null
        constraint avsnitt_pkey
            primary key,
    behandling_id uuid
        constraint avsnitt_behandling_id_fkey
            references behandling,
    deloverskrift varchar,
    innhold varchar,
    skal_skjules_i_brevbygger boolean,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null
);

create table if not exists person_ident
(
    ident varchar not null
        constraint person_ident_pkey
            primary key,
    fagsak_person_id uuid not null
        constraint person_ident_fagsak_person_id_fkey
            references fagsak_person,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null
);

create index if not exists person_ident_fagsak_person_id_idx
    on person_ident (fagsak_person_id);

create table if not exists distribusjon_resultat
(
    behandling_id uuid not null
        constraint distribusjon_resultat_pkey
            primary key
        constraint distribusjon_resultat_behandling_id_fkey
            references behandling,
    journalpost_id varchar,
    brev_distribusjon_id varchar,
    oversendt_til_kabal_tidspunkt timestamp(3) default LOCALTIMESTAMP,
    opprettet_av varchar default 'VL'::character varying not null,
    opprettet_tid timestamp(3) default LOCALTIMESTAMP not null,
    endret_av varchar not null,
    endret_tid timestamp default LOCALTIMESTAMP not null
);

create table if not exists klageresultat
(
    event_id uuid not null
        constraint klageresultat_pkey
            primary key,
    type varchar not null,
    utfall varchar,
    mottatt_eller_avsluttet_tidspunkt timestamp(3) not null,
    kildereferanse uuid not null,
    journalpost_referanser varchar,
    behandling_id uuid not null
        constraint klageresultat_behandling_id_fkey
            references behandling
);

create index if not exists klageresultat_behandling_id_idx
    on klageresultat (behandling_id);