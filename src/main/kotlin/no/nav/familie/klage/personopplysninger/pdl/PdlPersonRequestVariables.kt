package no.nav.familie.klage.personopplysninger.pdl

data class PdlPersonRequestVariables(val ident: String)

data class PdlIdentRequestVariables(
    val ident: String,
    val gruppe: String,
    val historikk: Boolean = false
)
