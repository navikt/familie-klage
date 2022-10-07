package no.nav.familie.klage.brev.dto

import no.nav.familie.klage.brev.domain.Avsnitt
import java.util.UUID

data class AvsnittDto(
    val avsnittId: UUID,
    val deloverskrift: String,
    val innhold: String,
    val skalSkjulesIBrevbygger: Boolean? = false
)

fun Avsnitt.tilDto(): AvsnittDto = AvsnittDto(
    avsnittId = avsnittId,
    deloverskrift = deloverskrift,
    innhold = innhold,
    skalSkjulesIBrevbygger = skalSkjulesIBrevbygger
)
