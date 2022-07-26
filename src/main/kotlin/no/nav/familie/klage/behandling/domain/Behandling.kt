package no.nav.familie.klage.behandling.domain

import no.nav.familie.klage.felles.domain.BehandlerRolle
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
    val personId: String,
    val steg: StegType,
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
    VENTER,
    FERDIGSTILT
}
enum class StegTypeNavn {
    OPPRETTET,
    FORMKRAV,
    VURDERING,
    BREV,
    SEND_TIL_BESLUTTER,
    VENTE_PÅ_SVAR_FRA_BESLUTTER,
    BEHANDLING_FERDIGSTILT
}
/*
enum class Fagsystem {
    EF,
    BA,
    KS
}*/

enum class StegType(
    val rekkefølge: Int,
    val tillattFor: BehandlerRolle,
    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>
) {
    OPPRETTET(
        rekkefølge = 0,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET)
    ),
    FORMKRAV(
        rekkefølge = 1,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES)
    ),
    VURDERING(
        rekkefølge = 2,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)
    ),
    BREV(
        rekkefølge = 3,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)
    ),
    SEND_TIL_BESLUTTER(
        rekkefølge = 4,
        tillattFor = BehandlerRolle.SAKSBEHANDLER,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)
    ),
    VENTE_PÅ_SVAR_FRA_BESLUTTER(
        rekkefølge = 5,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.VENTER)
    ),
    BEHANDLING_FERDIGSTILT(
        rekkefølge = 6,
        tillattFor = BehandlerRolle.SYSTEM,
        gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT)
    );

    fun hentNesteSteg(): StegType {
        return when (this) {
            OPPRETTET -> FORMKRAV
            FORMKRAV -> VURDERING
            VURDERING -> BREV
            BREV -> SEND_TIL_BESLUTTER
            SEND_TIL_BESLUTTER -> VENTE_PÅ_SVAR_FRA_BESLUTTER
            VENTE_PÅ_SVAR_FRA_BESLUTTER -> BEHANDLING_FERDIGSTILT
            BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT
        }
    }
}

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