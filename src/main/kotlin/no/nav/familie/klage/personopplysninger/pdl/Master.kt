package no.nav.familie.klage.personopplysninger.pdl

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class Master(
    val prioritet: Int,
) {
    PDL(1),
    FREG(2),
    UKJENT(3),
    ;

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Master::class.java)

        fun fraVerdi(verdi: String): Master =
            when (verdi.uppercase()) {
                "PDL" -> {
                    PDL
                }

                "FREG" -> {
                    FREG
                }

                else -> {
                    logger.warn("Ukjent verdi for master: $verdi, burde legges til i Master enum")
                    UKJENT
                }
            }
    }
}
