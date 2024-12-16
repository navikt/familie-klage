package no.nav.familie.klage.behandling.dto

import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import java.time.LocalDate

data class OppgaveDto(
    val oppgaveId: Long? = null,
    val beskrivelse: String? = null,
    val tilordnetRessurs: String,
    val prioritet: OppgavePrioritet? = null,
    val fraFristDato: String,
    val fristFerdigstillelse: String,
    val mappeId: Long? = null,
)
