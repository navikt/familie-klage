package no.nav.familie.klage.personopplysninger.domain

data class PersonopplysningerDto(
    val personIdent: String,
    val navn: String,
    val kjønn: Kjønn,
    val telefonnummer: String,
    val adresse: String
)

enum class Kjønn {
    KVINNE,
    MANN,
    UKJENT
}
