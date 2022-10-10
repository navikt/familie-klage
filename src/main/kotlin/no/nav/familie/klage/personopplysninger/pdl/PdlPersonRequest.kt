package no.nav.familie.klage.personopplysninger.pdl

data class PdlPersonRequest(
    val variables: PdlPersonRequestVariables,
    val query: String
)

data class PdlIdentRequest(
    val variables: PdlIdentRequestVariables,
    val query: String
)
