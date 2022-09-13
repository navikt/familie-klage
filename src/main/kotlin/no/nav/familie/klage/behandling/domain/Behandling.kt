package no.nav.familie.klage.behandling.domain

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val steg: StegType = StegType.FORMKRAV,
    val status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val resultat: BehandlingResultat? = BehandlingResultat.IKKE_SATT,
    val vedtakDato: LocalDateTime? = null,
    val eksternBehandlingId: String,
    val klageMottatt: LocalDate,
    val behandlendeEnhet: String
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
    FERDIGSTILT;

    fun erLåstForVidereBehandling(): Boolean = listOf(VENTER, FERDIGSTILT).contains(this)
}

enum class StegType(
    val rekkefølge: Int,
    val gjelderStatus: BehandlingStatus
) {
    FORMKRAV(
        rekkefølge = 1,
        gjelderStatus = BehandlingStatus.UTREDES
    ),
    VURDERING(
        rekkefølge = 2,
        gjelderStatus = BehandlingStatus.UTREDES
    ),
    BREV(
        rekkefølge = 3,
        gjelderStatus = BehandlingStatus.UTREDES
    ),
    OVERFØRING_TIL_KABAL(
        rekkefølge = 4,
        gjelderStatus = BehandlingStatus.VENTER
    ),
    BEHANDLING_FERDIGSTILT(
        rekkefølge = 6,
        gjelderStatus = BehandlingStatus.FERDIGSTILT
    );
}
