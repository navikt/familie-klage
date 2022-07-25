package no.nav.familie.klage.behandling.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.Fagsystem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val steg: BehandlingSteg,
    val status: BehandlingStatus,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val resultat: BehandlingResultat? = BehandlingResultat.IKKE_SATT,
    val fagsystem: Fagsystem,
    val vedtakDato: LocalDateTime? = null,
    val stonadsType: StønadsType,
    val behandlingsArsak: BehandlingsÅrsak,
)

enum class BehandlingResultat(val displayName: String) {
    MEDHOLD(displayName = "Medhold"),
    IKKE_MEDHOLD(displayName = "Ikke medhold"),
    IKKE_SATT(displayName = "Ikke satt"),
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FERDIGSTILT
}

enum class BehandlingSteg {
    FORMALKRAV,
    VURDERING,
    KABAL,
    BEHANDLING_FERDIGSTILT
}
/*
enum class Fagsystem {
    EF,
    BA,
    KS
}*/

enum class StønadsType {
    OVERGANGSSTØNAD,
    SKOLEPENGER,
    BARNETILSYN
}

enum class BehandlingsÅrsak {
    KLAGE,
    NYE_OPPLYSNINGER,
    SANKSJON_1_MND,
    SØKNAD,
    MIGRERING,
    G_OMREGNING,
    KORRIGERING_UTEN_BREV,
    PAPIRSØKNAD
}