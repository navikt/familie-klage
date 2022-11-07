package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val formService: FormService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskRepository: TaskRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettBehandling(
        opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest
    ): UUID {
        val klageMottatt = opprettKlagebehandlingRequest.klageMottatt
        val stønadstype = opprettKlagebehandlingRequest.stønadstype
        val eksternFagsakId = opprettKlagebehandlingRequest.eksternFagsakId

        feilHvis(klageMottatt.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for behandling med eksternBehandlingId=${opprettKlagebehandlingRequest.eksternBehandlingId}"
        }

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            ident = opprettKlagebehandlingRequest.ident,
            eksternId = eksternFagsakId,
            fagsystem = opprettKlagebehandlingRequest.fagsystem,
            stønadstype = stønadstype
        )

        val behandlingId = behandlingService.opprettBehandling(
            Behandling(
                fagsakId = fagsak.id,
                påklagetVedtak = PåklagetVedtak(
                    eksternFagsystemBehandlingId = opprettKlagebehandlingRequest.eksternBehandlingId,
                    påklagetVedtakstype = if (opprettKlagebehandlingRequest.eksternBehandlingId != null) PåklagetVedtakstype.VEDTAK else PåklagetVedtakstype.IKKE_VALGT
                ),
                klageMottatt = klageMottatt,
                behandlendeEnhet = opprettKlagebehandlingRequest.behandlendeEnhet
            )
        ).id

        behandlingshistorikkService.opprettBehandlingshistorikk(behandlingId, StegType.OPPRETTET)

        formService.opprettInitielleFormkrav(behandlingId)

        oppgaveTaskService.opprettBehandleSakOppgave(behandlingId)
        taskRepository.save(
            BehandlingsstatistikkTask.opprettMottattTask(behandlingId = behandlingId)
        )
        logger.info(
            "Opprettet behandling=$behandlingId for stønadstype=$stønadstype " +
                "eksternFagsakId=$eksternFagsakId klageMottatt=$klageMottatt"
        )
        return behandlingId
    }
}
