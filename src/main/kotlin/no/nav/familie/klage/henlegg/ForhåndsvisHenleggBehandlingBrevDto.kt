package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerDto

data class ForhåndsvisHenleggBehandlingBrevDto(
    val brevmottakere: List<NyBrevmottakerDto>,
)
