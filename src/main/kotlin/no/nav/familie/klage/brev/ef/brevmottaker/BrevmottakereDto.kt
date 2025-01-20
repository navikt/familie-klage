package no.nav.familie.klage.brev.ef.brevmottaker

import no.nav.familie.klage.brev.ef.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.ef.BrevmottakerPerson
import no.nav.familie.klage.brev.ef.Brevmottakere

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
