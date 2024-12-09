package no.nav.familie.klage.behandling.dto

import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet

data class OppgaveDto(
    val tilordnetRessurs: String,
    val prioritet: OppgavePrioritet,
    val fristFerdigstillelse: String,
    val mappeId: Long,
)
