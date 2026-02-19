package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettKabalEventOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave for relevant hendelse fra kabal",
)
class OpprettKabalEventOppgaveTask(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personRepository: FagsakPersonRepository,
    private val oppgaveClient: OppgaveClient,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val opprettOppgavePayload = jsonMapper.readValue(task.payload, OpprettOppgavePayload::class.java)

        val behandling = behandlingRepository.findByEksternBehandlingId(opprettOppgavePayload.klagebehandlingEksternId)

        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id)
        if (fagsakDomain == null) {
            logger.error("Finner ikke fagsak for behandling med ekstern id ${behandling.eksternBehandlingId}.")
            throw Feil("Finner ikke fagsak for behandling med ekstern id ${behandling.eksternBehandlingId}.")
        }

        val aktivIdent = personRepository.hentAktivIdent(fagsakDomain.fagsakPersonId)

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident =
                    OppgaveIdentV2(
                        ident = aktivIdent,
                        gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    ),
                saksId = fagsakDomain.eksternId,
                tema = fagsakDomain.stønadstype.tilTema(),
                oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = opprettOppgavePayload.oppgaveTekst,
                enhetsnummer = behandling.behandlendeEnhet,
                behandlingstema = opprettOppgavePayload.behandlingstema?.value,
                behandlingstype = opprettOppgavePayload.behandlingstype,
                prioritet = utledOppgavePrioritet(opprettOppgavePayload.klageinstansUtfall),
            )

        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)

        logger.info("Oppgave opprettet med id $oppgaveId for behandling med ekstern id ${behandling.eksternBehandlingId}")
    }

    companion object {
        const val TYPE = "opprettOppgaveForKlagehendelse"

        fun opprettTask(
            opprettOppgavePayload: OpprettOppgavePayload,
            eksternFagsakId: String,
            fagsystem: Fagsystem,
        ): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(opprettOppgavePayload),
                properties =
                    Properties().apply {
                        this["eksternFagsakId"] = eksternFagsakId
                        this["fagsystem"] = fagsystem.name
                    },
            )
    }

    private fun utledOppgavePrioritet(klageinstansUtfall: KlageinstansUtfall?): OppgavePrioritet =
        when (klageinstansUtfall) {
            KlageinstansUtfall.OPPHEVET -> OppgavePrioritet.HOY
            else -> OppgavePrioritet.NORM
        }
}

data class OpprettOppgavePayload(
    val klagebehandlingEksternId: UUID,
    val oppgaveTekst: String,
    val fagsystem: Fagsystem,
    val klageinstansUtfall: KlageinstansUtfall?,
    val behandlingstema: Behandlingstema? = null,
    val behandlingstype: String? = null,
)
