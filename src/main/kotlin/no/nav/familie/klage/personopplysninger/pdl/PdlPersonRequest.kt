package no.nav.familie.klage.personopplysninger.pdl

data class PdlPersonRequest(
    val variables: PdlPersonRequestVariables,
    val query: String
)

data class PdlPersonBolkRequest(
    val variables: PdlPersonBolkRequestVariables,
    val query: String
)

data class PdlIdentRequest(
    val variables: PdlIdentRequestVariables,
    val query: String
)

data class PdlIdentBolkRequest(
    val variables: PdlIdentBolkRequestVariables,
    val query: String
)

data class PdlPersonSøkRequest(
    val variables: PdlPersonSøkRequestVariables,
    val query: String
)
