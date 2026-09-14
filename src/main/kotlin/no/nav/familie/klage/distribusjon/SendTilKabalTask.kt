package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendTilKabalTask.TYPE,
    beskrivelse = "Send klage til kabal",
)
class SendTilKabalTask(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val kabalService: KabalService,
    private val vurderingService: VurderingService,
    private val brevService: BrevService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandlerIdent = task.metadata[TaskMetadata.SAKSBEHANDLER_METADATA_KEY].toString()
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        val vurdering =
            vurderingService.hentVurdering(behandlingId)
                ?: error("Mangler vurdering på klagen - kan ikke oversendes til kabal")

        val brevmottakere =
            if (behandling.årsak != Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL) {
                brevService.hentBrevmottakere(behandlingId)
            } else {
                null
            }

        try {
            kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerIdent, brevmottakere)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                logger.warn("409 conflict ved sending av klage til Kabal. Gjelder behandlingId=$behandlingId")
            } else {
                throw e
            }
        }
    }

    override fun onCompletion(task: Task) {
        val oversendtTidspunkt = LocalDateTime.now()
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)

        if (behandling.resultat != BehandlingResultat.IKKE_MEDHOLD) {
            logger.info(
                "Sender ikke statistikk om oversendelse til KA for behandlingId=$behandlingId med resultat=${behandling.resultat}",
            )
            return
        }

        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        taskService.save(
            BehandlingsstatistikkTask.opprettSendtTilKATask(
                behandlingId = behandlingId,
                hendelseTidspunkt = oversendtTidspunkt,
                gjeldendeSaksbehandler = task.metadata[TaskMetadata.SAKSBEHANDLER_METADATA_KEY].toString(),
                eksternFagsakId = fagsak.eksternId,
                fagsystem = fagsak.fagsystem,
            ),
        )
    }

    companion object {
        const val TYPE = "sendTilKabalTask"
    }
}
