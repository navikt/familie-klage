package no.nav.familie.klage.behandling.dto

data class OppdaterBehandlendeEnhetRequest(
    val enhetsnummer: String,
    val begrunnelse: String
)
