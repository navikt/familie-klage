package no.nav.familie.klage.kabal.event

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.oppgave.OppgaveClient
import no.nav.familie.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave for relevant hendelse fra kabal"
)
class OpprettOppgaveTask(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personRepository: FagsakPersonRepository,
    private val oppgaveClient: OppgaveClient
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(task.payload)
        val behandling = behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId)
        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        val personId = fagsakDomain?.fagsakPersonId
            ?: throw Feil("Feil ved henting av aktiv ident: Finner ikke fagsak for behandling med klagebehandlingEksternId ${opprettOppgavePayload.klagebehandlingEksternId}")

        val aktivIdent = personRepository.hentAktivIdent(personId)

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktivIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksId = fagsakDomain.eksternId,
                tema = fagsakDomain.stønadstype.tilTema(),
                oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = opprettOppgavePayload.oppgaveTekst,
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstema = finnBehandlingstema(fagsakDomain.stønadstype).value
            )

        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        logger.info("Oppgave opprettet med id $oppgaveId")
    }

    companion object {

        const val TYPE = "opprettOppgaveForKlagehendelse"

        fun opprettTask(opprettOppgavePayload: OpprettOppgavePayload): Task {
            return Task(
                TYPE,
                objectMapper.writeValueAsString(opprettOppgavePayload)
            )
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
}

data class OpprettOppgavePayload(
    val klagebehandlingEksternId: UUID,
    val oppgaveTekst: String,
    val fagsystem: Fagsystem
)
