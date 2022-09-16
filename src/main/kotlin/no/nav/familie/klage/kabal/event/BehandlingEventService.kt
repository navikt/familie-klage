package no.nav.familie.klage.kabal.event

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.integrasjoner.OppgaveClient
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
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

@Service
class BehandlingEventService(
    private val oppgaveClient: OppgaveClient,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val personRepository: FagsakPersonRepository,
    private val stegService: StegService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handleEvent(behandlingEvent: BehandlingEvent) {
        val behandling = behandlingRepository.findByEksternBehandlingIdAndFagsystem(
            behandlingEvent.kildeReferanse,
            behandlingEvent.kilde
        )

        when (behandlingEvent.type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> behandleKlageAvsluttet(behandling, behandlingEvent)
            else -> behandleAnke(behandling, behandlingEvent)
        }
    }

    private fun behandleAnke(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        opprettOppgave(behandlingEvent, behandling)
    }

    private fun behandleKlageAvsluttet(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        when (behandling.status) {
            BehandlingStatus.FERDIGSTILT -> logger.error("Mottatt event på ferdigstilt behandling $behandlingEvent") // TODO korrigeringer - kan vi få det?
            else -> {
                opprettOppgave(behandlingEvent, behandling)
                ferdigstillKlagebehandling(behandling)
            }
        }
    }

    private fun opprettOppgave(behandlingEvent: BehandlingEvent, behandling: Behandling) {
        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        val personId = fagsakDomain?.fagsakPersonId
            ?: throw Feil("Feil ved henting av aktiv ident: Finner ikke fagsak for behandling med eksternId ${behandlingEvent.kildeReferanse}")

        val aktivIdent = personRepository.hentAktivIdent(personId)

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktivIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksId = fagsakDomain.eksternId,
                tema = fagsakDomain.stønadstype.tilTema(),
                oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = " ${behandlingEvent.detaljer.oppgaveTekst()} Gjelder: ${fagsakDomain.stønadstype}",
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstema = finnBehandlingstema(fagsakDomain.stønadstype).value
            )

        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        logger.info("Oppgave opprettet med id $oppgaveId")
    }

    private fun ferdigstillKlagebehandling(behandling: Behandling) {
        stegService.oppdaterSteg(behandling.id, StegType.BEHANDLING_FERDIGSTILT)
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
