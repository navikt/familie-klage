package no.nav.familie.klage.brev.dto

import no.nav.familie.klage.brev.domain.Avsnitt

data class AvsnittDto(
    val deloverskrift: String,
    val deloverskriftHeading: Heading? = null,
    val innhold: String,
    val skalSkjulesIBrevbygger: Boolean? = false,
)

fun Avsnitt.tilDto(): AvsnittDto =
    AvsnittDto(
        deloverskrift = deloverskrift,
        innhold = innhold,
        skalSkjulesIBrevbygger = skalSkjulesIBrevbygger,
    )

enum class Heading {
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,
}
