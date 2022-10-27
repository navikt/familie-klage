package no.nav.familie.klage.brev.dto

import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.domain.Brevmottakere

data class BrevmottakereDto(
    val personer: List<BrevmottakerPerson>,
    val organisasjoner: List<BrevmottakerOrganisasjon>
)

fun Brevmottakere.tilDto() = BrevmottakereDto(
    personer = this.personer,
    organisasjoner = this.organisasjoner
)

fun BrevmottakereDto.tilDomene() = Brevmottakere(
    personer = this.personer,
    organisasjoner = this.organisasjoner
)