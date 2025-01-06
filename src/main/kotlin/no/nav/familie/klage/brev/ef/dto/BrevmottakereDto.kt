package no.nav.familie.klage.brev.ef.dto

import no.nav.familie.klage.brev.ef.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.ef.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.ef.domain.Brevmottakere

data class BrevmottakereDto(
    val personer: List<BrevmottakerPerson>,
    val organisasjoner: List<BrevmottakerOrganisasjon>,
)

fun Brevmottakere.tilDto() = BrevmottakereDto(
    personer = this.personer,
    organisasjoner = this.organisasjoner,
)

fun BrevmottakereDto.tilDomene() = Brevmottakere(
    personer = this.personer,
    organisasjoner = this.organisasjoner,
)
