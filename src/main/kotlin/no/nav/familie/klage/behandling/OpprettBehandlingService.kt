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
import no.nav.familie.prosessering.internal.TaskService
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
    private val taskService: TaskService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettBehandling(
        request: OpprettKlagebehandlingRequest,
    ): UUID {
        val klageMottatt = request.klageMottatt
        val stønadstype = request.stønadstype
        val eksternFagsakId = request.eksternFagsakId

        feilHvis(klageMottatt.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for eksternFagsakId=$eksternFagsakId"
        }

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            ident = request.ident,
            eksternId = eksternFagsakId,
            fagsystem = request.fagsystem,
            stønadstype = stønadstype,
        )

        val behandlingId = behandlingService.opprettBehandling(
            Behandling(
                fagsakId = fagsak.id,
                påklagetVedtak = PåklagetVedtak(
                    påklagetVedtakstype = PåklagetVedtakstype.IKKE_VALGT,
                ),
                klageMottatt = klageMottatt,
                behandlendeEnhet = request.behandlendeEnhet,
                årsak = request.behandlingsårsak,
            ),
        ).id

        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandlingId,
            steg = StegType.OPPRETTET,
        )

        formService.opprettInitielleFormkrav(behandlingId)

        oppgaveTaskService.opprettBehandleSakOppgave(
            behandlingId = behandlingId,
            klageGjelderTilbakekreving = request.klageGjelderTilbakekreving,
            eksternFagsakId = eksternFagsakId,
            fagsystem = fagsak.fagsystem,
        )
        taskService.save(
            BehandlingsstatistikkTask.opprettMottattTask(
                behandlingId = behandlingId,
                eksternFagsakId = eksternFagsakId,
                fagsystem = fagsak.fagsystem,

            ),
        )
        logger.info(
            "Opprettet behandling=$behandlingId for stønadstype=$stønadstype " +
                "eksternFagsakId=$eksternFagsakId klageMottatt=$klageMottatt",
        )
        return behandlingId
    }
}
