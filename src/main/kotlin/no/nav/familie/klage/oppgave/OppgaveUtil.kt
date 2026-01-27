package no.nav.familie.klage.oppgave

import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import java.time.LocalDate
import java.time.LocalDateTime

object OppgaveUtil {
    const val ENHET_NR_NAY = "4489"
    const val ENHET_NR_EGEN_ANSATT = "4483"

    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val fristTilNesteVirkedag = nesteVirkedag(gjeldendeTid.toLocalDate())
        return if (gjeldendeTid.hour >= 12) {
            nesteVirkedag(fristTilNesteVirkedag)
        } else {
            fristTilNesteVirkedag
        }
    }
}
