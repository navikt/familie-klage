package no.nav.familie.klage.personopplysninger.dto

data class PersonopplysningerDto(
    val personIdent: String,
    val navn: String,
    val kjønn: Kjønn
)

enum class Kjønn {
    KVINNE,
    MANN,
    UKJENT
}
