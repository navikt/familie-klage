package no.nav.familie.klage.felles.util

object TekstUtil {

    fun String.storForbokstav() = this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
}
