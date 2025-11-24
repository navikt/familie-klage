package no.nav.familie.klage.porteføljejustering

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.enhet.Enhet.Companion.finnEnhet
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask.Companion.opprettPåbegyntTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.BehandleSakOppgaveRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/portefoljejustering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PorteføljejusteringController(
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
) {
    @PutMapping("/oppdater-behandlende-enhet")
    fun oppdaterBehandlendeEnhetPåBehandling(
        @RequestBody oppdaterBehandlendeEnhetRequest: OppdaterBehandlendeEnhetRequest,
    ): Ressurs<String> {
        val oppgaveId = oppdaterBehandlendeEnhetRequest.oppgaveId
        val nyEnhet = oppdaterBehandlendeEnhetRequest.nyEnhet
        val fagsystem = oppdaterBehandlendeEnhetRequest.fagsystem

        val behandleSakOppgave =
            behandleSakOppgaveRepository.findByOppgaveId(oppgaveId)
                ?: throw IllegalStateException("Fant ikke BehandleSakOppgave for oppgaveId=$oppgaveId")

        val behandling = behandlingService.hentBehandling(behandleSakOppgave.behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val enhet = finnEnhet(fagsystem, nyEnhet)

        val enhetErOppdatert = enhet.enhetsnummer == behandling.behandlendeEnhet
        if (enhetErOppdatert) {
            return Ressurs.success("Behandlende enhet er allerede satt til ${enhet.enhetsnummer}. Ingen oppdatering gjort.")
        }

        behandlingService.oppdaterBehandlendeEnhet(
            behandlingId = behandling.id,
            behandlendeEnhet = enhet,
            fagsystem = fagsystem,
        )

        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET,
            beskrivelse = "Behandlende enhet endret i forbindelse med porteføljejustering.",
        )

        taskService.save(
            opprettPåbegyntTask(
                behandlingId = behandling.id,
                eksternFagsakId = fagsak.eksternId,
                fagsystem = fagsystem,
                gjeldendeSaksbehandler = SikkerhetContext.SYSTEM_FORKORTELSE,
            ),
        )

        return Ressurs.success("Behandlende enhet oppdatert til ${enhet.enhetsnummer}.")
    }

    data class OppdaterBehandlendeEnhetRequest(
        val oppgaveId: Long,
        val nyEnhet: String,
        val fagsystem: Fagsystem,
    )
}
