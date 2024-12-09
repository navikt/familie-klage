package no.nav.familie.klage.behandling.dto

import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet

data class SettPåVentRequest(
    val oppgaveId: Long,
    val saksbehandler: String,
    val prioritet: OppgavePrioritet,
    val frist: String,
    val mappe: Long?,
    val beskrivelse: String,
    val oppgaveVersjon: Int,
//    val oppfølgingsoppgaverMotLokalKontor: List<OppgaveSubtype>,
    val innstillingsoppgaveBeskjed: String?,
)
