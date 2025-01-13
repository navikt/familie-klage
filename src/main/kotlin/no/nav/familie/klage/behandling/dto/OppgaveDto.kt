package no.nav.familie.klage.behandling.dto

import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet

data class OppgaveDto(
    val oppgaveId: Long? = null,
    val tildeltEnhetsnr: String? = null,
    val beskrivelse: String? = null,
    val tilordnetRessurs: String,
    val prioritet: OppgavePrioritet? = null,
    val fristFerdigstillelse: String,
    val mappeId: Long? = null,
)
