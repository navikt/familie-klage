package no.nav.familie.klage.behandling.domain

import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import java.time.LocalDateTime

data class FagsystemRevurdering(
    val opprettetBehandling: Boolean,
    val opprettet: Opprettet? = null,
    val ikkeOpprettet: IkkeOpprettet? = null,
) {
    init {
        if (opprettetBehandling) {
            require(opprettet != null) { "opprettet mangler" }
            require(ikkeOpprettet == null) { "ikkeOpprettet må være null" }
        } else {
            require(ikkeOpprettet != null) { "ikkeOpprettet mangler" }
            require(opprettet == null) { "opprettet må være null" }
        }
    }
}

data class Opprettet(
    val eksternBehandlingId: String,
    val opprettetTid: LocalDateTime,
)

data class IkkeOpprettet(
    val årsak: IkkeOpprettetÅrsak,
    val detaljer: String? = null,
)

enum class IkkeOpprettetÅrsak {
    ÅPEN_BEHANDLING,
    INGEN_BEHANDLING,
    FEIL,
}

fun OpprettRevurderingResponse.tilFagsystemRevurdering(): FagsystemRevurdering {
    val opprettet = this.opprettet?.let { Opprettet(it.eksternBehandlingId, opprettetTid = LocalDateTime.now()) }
    val ikkeOpprettet =
        this.ikkeOpprettet?.let {
            IkkeOpprettet(
                IkkeOpprettetÅrsak.valueOf(it.årsak.name),
                it.detaljer,
            )
        }
    return FagsystemRevurdering(
        opprettetBehandling = this.opprettetBehandling,
        opprettet = opprettet,
        ikkeOpprettet = ikkeOpprettet,
    )
}
