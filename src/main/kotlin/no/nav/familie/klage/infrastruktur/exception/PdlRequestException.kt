package no.nav.familie.klage.infrastruktur.exception

open class PdlRequestException(
    melding: String? = null,
) : Exception(melding)

class PdlNotFoundException : PdlRequestException()
