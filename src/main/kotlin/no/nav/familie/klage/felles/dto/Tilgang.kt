package no.nav.familie.klage.felles.dto

data class Tilgang(
    val harTilgang: Boolean,
    val begrunnelse: String? = null,
) {

    fun utledÅrsakstekst(): String = when (this.begrunnelse) {
        null -> ""
        else -> "Årsak: ${this.begrunnelse}"
    }
}
