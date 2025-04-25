package no.nav.familie.klage.felles.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TekstUtil {
    fun String.storForbokstav() = this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }

    object DatoFormat {
        val DATE_FORMAT_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val DATE_FORMAT_NORSK_LANG = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.of("no"))
        val GOSYS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy' 'HH:mm")
    }

    fun LocalDate.norskFormat() = this.format(DatoFormat.DATE_FORMAT_NORSK)

    fun LocalDate.norskFormatLang() = this.format(DatoFormat.DATE_FORMAT_NORSK_LANG)

    fun LocalDateTime.norskFormat() = this.format(DatoFormat.DATE_FORMAT_NORSK)

    fun LocalDateTime.norskFormatLang() = this.format(DatoFormat.DATE_FORMAT_NORSK_LANG)
}
