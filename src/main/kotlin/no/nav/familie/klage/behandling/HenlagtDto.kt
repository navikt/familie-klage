package no.nav.familie.klage.behandling.dto

data class HenlagtDto(val årsak: HenlagtÅrsak)

enum class HenlagtÅrsak {
    TRUKKET_TILBAKE,
    FEILREGISTRERT
}
