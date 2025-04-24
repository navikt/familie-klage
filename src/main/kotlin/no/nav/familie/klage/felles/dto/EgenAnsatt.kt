package no.nav.familie.klage.felles.dto

data class EgenAnsattRequest(
    val ident: String,
)

data class EgenAnsattResponse(
    val erEgenAnsatt: Boolean,
)
