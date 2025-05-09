package no.nav.familie.klage.blankett

import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagSaksbehandlingsblankettTask.TYPE,
    beskrivelse = "Lager og journalfører blankett",
)
class LagSaksbehandlingsblankettTask(
    private val blankettService: BlankettService,
    private val distribusjonService: DistribusjonService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val blankettPdf = blankettService.lagBlankett(behandlingId)
        val journalpostId =
            distribusjonService.journalførSaksbehandlingsblankett(
                behandlingId,
                blankettPdf,
                task.metadata.getProperty(SAKSBEHANDLER_METADATA_KEY),
            )

        logger.info("Lagret saksbehandlingsblankett for behandling=$behandlingId på journapost=$journalpostId")
    }

    companion object {
        const val TYPE = "LagSaksbehandlingsblankett"

        fun opprettTask(
            behandlingId: UUID,
            eksternFagsakId: String,
            fagsystem: Fagsystem,
        ): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = SikkerhetContext.hentSaksbehandler(strict = true)
                        this["eksternFagsakId"] = eksternFagsakId
                        this["fagsystem"] = fagsystem.name
                    },
            )
    }
}
