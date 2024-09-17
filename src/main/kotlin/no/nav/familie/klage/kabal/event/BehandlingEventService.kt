package no.nav.familie.klage.kabal.event

import no.nav.familie.http.client.RessursException
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.BehandlingFeilregistrertTask
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.kabal.domain.KlageinstansResultat
import no.nav.familie.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.familie.klage.oppgave.OpprettOppgavePayload
import no.nav.familie.klage.personopplysninger.pdl.secureLogger
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingEventService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
    private val klageresultatRepository: KlageresultatRepository,
    private val stegService: StegService,
    private val integrasjonerClient: FamilieIntegrasjonerClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleEvent(behandlingEvent: BehandlingEvent) {
        val finnesKlageresultat = klageresultatRepository.existsById(behandlingEvent.eventId)
        if (finnesKlageresultat) {
            logger.warn("Hendelse fra kabal med eventId: ${behandlingEvent.eventId} er allerede lest - prosesserer ikke hendelse.")
        } else {
            logger.info("Prosesserer hendelse fra kabal med eventId: ${behandlingEvent.eventId}")
            val eksternBehandlingId = UUID.fromString(behandlingEvent.kildeReferanse)
            val behandling = behandlingRepository.findByEksternBehandlingId(eksternBehandlingId)

            lagreKlageresultat(behandlingEvent, behandling)

            when (behandlingEvent.type) {
                BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> behandleKlageAvsluttet(behandling, behandlingEvent)
                BehandlingEventType.ANKEBEHANDLING_AVSLUTTET,
                -> behandleAnkeAvsluttet(behandling, behandlingEvent)

                BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
                BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET,
                BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET,
                -> {
                    /*
                     * Skal ikke gjøre noe dersom eventtype er ANKEBEHANDLING_OPPRETTET,
                     * ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET eller BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET
                     * */
                }
                BehandlingEventType.BEHANDLING_FEILREGISTRERT -> opprettBehandlingFeilregistrertTask(behandling.id)
            }
        }
    }

    private fun opprettBehandlingFeilregistrertTask(behandlingId: UUID) {
        taskService.save(BehandlingFeilregistrertTask.opprettTask(behandlingId))
    }

    private fun lagreKlageresultat(behandlingEvent: BehandlingEvent, behandling: Behandling) {
        val klageinstansResultat = KlageinstansResultat(
            eventId = behandlingEvent.eventId,
            type = behandlingEvent.type,
            utfall = behandlingEvent.utfall(),
            mottattEllerAvsluttetTidspunkt = behandlingEvent.mottattEllerAvsluttetTidspunkt(),
            kildereferanse = UUID.fromString(behandlingEvent.kildeReferanse),
            journalpostReferanser = StringListWrapper(behandlingEvent.journalpostReferanser()),
            behandlingId = behandling.id,
            årsakFeilregistrert = utledÅrsakFeilregistrert(behandlingEvent),
        )

        klageresultatRepository.insert(klageinstansResultat)
    }

    private fun utledÅrsakFeilregistrert(behandlingEvent: BehandlingEvent) =
        if (behandlingEvent.type == BehandlingEventType.BEHANDLING_FEILREGISTRERT) {
            behandlingEvent.detaljer.behandlingFeilregistrert?.reason
                ?: error("Finner ikke årsak til feilregistrering")
        } else {
            null
        }

    private fun behandleAnkeAvsluttet(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        opprettOppgaveTask(behandling, behandlingEvent)
    }

    private fun behandleKlageAvsluttet(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        when (behandling.status) {
            BehandlingStatus.FERDIGSTILT -> logger.error("Mottatt event på ferdigstilt behandling $behandlingEvent - event kan være lest fra før")
            else -> {
                opprettOppgaveTask(behandling, behandlingEvent)
                ferdigstillKlagebehandling(behandling)
            }
        }
    }

    private fun opprettOppgaveTask(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
            ?: error("Finner ikke fagsak for behandlingId: ${behandling.id}")
        val saksbehandlerIdent = behandling.sporbar.endret.endretAv
        val saksbehandlerEnhet = utledSaksbehandlerEnhet(saksbehandlerIdent)
        val oppgaveTekst = "${behandlingEvent.detaljer.oppgaveTekst(saksbehandlerEnhet)} Gjelder: ${fagsakDomain.stønadstype}"
        val klageBehandlingEksternId = UUID.fromString(behandlingEvent.kildeReferanse)
        val opprettOppgavePayload = OpprettOppgavePayload(
            klagebehandlingEksternId = klageBehandlingEksternId,
            oppgaveTekst = oppgaveTekst,
            fagsystem = fagsakDomain.fagsystem,
            klageinstansUtfall = behandlingEvent.utfall(),
            behandlingstema = finnBehandlingstema(fagsakDomain.stønadstype),
        )
        val opprettOppgaveTask = OpprettKabalEventOppgaveTask.opprettTask(opprettOppgavePayload)
        taskService.save(opprettOppgaveTask)
    }

    private fun utledSaksbehandlerEnhet(saksbehandlerIdent: String) =
        try {
            integrasjonerClient.hentSaksbehandlerInfo(saksbehandlerIdent).enhet
        } catch (e: RessursException) {
            logger.error("Kunne ikke hente enhet for saksbehandler med ident=$saksbehandlerIdent")
            secureLogger.error("Kunne ikke hente enhet for saksbehandler med ident=$saksbehandlerIdent", e)
            "Ukjent"
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

    private fun ferdigstillKlagebehandling(behandling: Behandling) {
        stegService.oppdaterSteg(behandling.id, StegType.KABAL_VENTER_SVAR, StegType.BEHANDLING_FERDIGSTILT)
    }
}
