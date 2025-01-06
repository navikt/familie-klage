package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.baks.mottaker.BrevmottakerService
import no.nav.familie.klage.brev.ef.BrevService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.Fagsystem
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
    private val brevService: BrevService,
    private val brevmottakerService: BrevmottakerService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandlerIdent = task.metadata[TaskMetadata.saksbehandlerMetadataKey].toString()
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val vurdering =
            vurderingService.hentVurdering(behandlingId)
                ?: error("Mangler vurdering på klagen - kan ikke oversendes til kabal")
        val brevmottakere = if (fagsak.fagsystem == Fagsystem.EF) {
            brevService.hentBrevmottakere(behandlingId)
        } else {
            brevmottakerService.hentBrevmottakere(behandlingId)
        }
        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerIdent, brevmottakere)
    }

    companion object {
        const val TYPE = "sendTilKabalTask"
    }
}
