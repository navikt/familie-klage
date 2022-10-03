package no.nav.familie.klage.oppgave

import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import java.time.LocalDate
import java.time.LocalDateTime

object OppgaveUtil {

    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val fristTilNesteVirkedag = nesteVirkedag(gjeldendeTid.toLocalDate())
        return if (gjeldendeTid.hour >= 12) {
            return nesteVirkedag(fristTilNesteVirkedag)
        } else {
            fristTilNesteVirkedag
        }
    }
}
