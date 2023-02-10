package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
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
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val vurdering =
            vurderingService.hentVurdering(behandlingId) ?: error("Mangler vurdering på klagen - kan ikke oversendes til kabal")
        kabalService.sendTilKabal(fagsak, behandling, vurdering)
    }

    companion object {
        const val TYPE = "sendTilKabalTask"
    }
}
