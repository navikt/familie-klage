package no.nav.familie.klage.brevmottaker.domain

data class Brevmottakere(
    val personer: List<BrevmottakerPerson> = emptyList(),
    val organisasjoner: List<BrevmottakerOrganisasjon> = emptyList(),
) {
    companion object {
        fun opprettFra(brevmottakere: List<NyBrevmottaker>): Brevmottakere =
            Brevmottakere(
                personer = brevmottakere.filterIsInstance<NyBrevmottakerPerson>().map { BrevmottakerPerson.opprettFra(it) },
                organisasjoner = brevmottakere.filterIsInstance<NyBrevmottakerOrganisasjon>().map { BrevmottakerOrganisasjon.opprettFra(it) },
            )
    }
}
