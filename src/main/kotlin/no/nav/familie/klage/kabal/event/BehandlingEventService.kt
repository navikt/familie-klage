package no.nav.familie.klage.kabal.event

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.integrasjoner.OppgaveClient
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingEventService(
    private val oppgaveClient: OppgaveClient,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val personRepository: FagsakPersonRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handleEvent(behandlingEvent: BehandlingEvent) {
        opprettOppgave(behandlingEvent)
        ferdigstillKlagebehandling()
    }

    private fun opprettOppgave(behandlingEvent: BehandlingEvent) {

        val behandling = behandlingRepository.findByEksternBehandlingId(UUID.fromString(behandlingEvent.kildeReferanse))
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        // TODO - hent aktørid
        val aktivIdent = personRepository.hentAktivIdent(
            fagsak?.fagsakPersonId
                ?: throw Feil("Finner ikke fagsak for behandling med eksternId ${behandlingEvent.kildeReferanse}")
        )

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktivIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksId = fagsak.eksternId,
                tema = Tema.ENF, // TODO finn riktig tema (BA/EF osv)
                oppgavetype = Oppgavetype.BehandleSak,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = behandlingEvent.detaljer.oppgaveTekst(),
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
                tilordnetRessurs = null,
                behandlesAvApplikasjon = "familie-ef-sak"
            )

        oppgaveClient.opprettOppgave(opprettOppgaveRequest)
    }

    fun ferdigstillKlagebehandling() {
    }

    private fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val frist = when (gjeldendeTid.dayOfWeek) {
            DayOfWeek.FRIDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2))
            DayOfWeek.SATURDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2).withHour(8))
            DayOfWeek.SUNDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(1).withHour(8))
            else -> fristBasertPåKlokkeslett(gjeldendeTid)
        }

        return when (frist.dayOfWeek) {
            DayOfWeek.SATURDAY -> frist.plusDays(2)
            DayOfWeek.SUNDAY -> frist.plusDays(1)
            else -> frist
        }
    }

    private fun finnBehandlingstema(stønadstype: Stønadstype): Behandlingstema {
        return when (stønadstype) {
            Stønadstype.BARNETRYGD -> Behandlingstema.Barnetrygd
            Stønadstype.OVERGANGSSTØNAD -> Behandlingstema.Overgangsstønad
            Stønadstype.BARNETILSYN -> Behandlingstema.Barnetilsyn
            Stønadstype.SKOLEPENGER -> Behandlingstema.Skolepenger
            Stønadstype.KONTANTSTØTTE -> Behandlingstema.Kontantstøtte
        }
    }

    private fun fristBasertPåKlokkeslett(gjeldendeTid: LocalDateTime): LocalDate {
        return if (gjeldendeTid.hour >= 12) {
            return gjeldendeTid.plusDays(2).toLocalDate()
        } else {
            gjeldendeTid.plusDays(1).toLocalDate()
        }
    }
}
