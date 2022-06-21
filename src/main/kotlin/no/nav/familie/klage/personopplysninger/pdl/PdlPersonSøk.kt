package no.nav.familie.klage.personopplysninger.pdl

data class PersonSøkResultat(
        val hits: List<PersonSøkTreff>,
        val totalHits: Int,
        val pageNumber: Int,
        val totalPages: Int
)

data class PersonSøkTreff(val person: PdlPersonFraSøk)

data class PdlPersonFraSøk(
        val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
        val bostedsadresse: List<Bostedsadresse>,
        val navn: List<Navn>
)

data class Folkeregisteridentifikator(val identifikasjonsnummer: String)

data class PersonSøk(val sokPerson: PersonSøkResultat)
