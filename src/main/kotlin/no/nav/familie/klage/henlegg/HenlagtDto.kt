package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak

data class HenlagtDto(
    val årsak: HenlagtÅrsak,
    val skalSendeHenleggelsesbrev: Boolean = false,
    val brevmottakere: Brevmottakere? = null,
)
