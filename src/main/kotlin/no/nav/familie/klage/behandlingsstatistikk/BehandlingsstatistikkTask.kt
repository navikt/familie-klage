package no.nav.familie.klage.behandlingsstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingsstatistikkTask.TYPE,
    beskrivelse = "Sender behandlingsstatistikk til iverksett",
    maxAntallFeil = 4,
    settTilManuellOppfølgning = true,
)
class BehandlingsstatistikkTask(
    private val behandlingStatistikkService: BehandlingsstatistikkService,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler) =
            objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)
        behandlingStatistikkService.sendBehandlingstatistikk(
            behandlingId,
            hendelse,
            hendelseTidspunkt,
            gjeldendeSaksbehandler,
        )
    }

    companion object {

        fun opprettMottattTask(behandlingId: UUID, eksternFagsakId: String, eksternBehandlingId: String, fagsystem: Fagsystem): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.MOTTATT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(),
                eksternFagsakId = eksternFagsakId,
                eksternBehandlingId = eksternBehandlingId,
                fagsystem = fagsystem
            )

        fun opprettPåbegyntTask(behandlingId: UUID, eksternFagsakId: String, eksternBehandlingId: String, fagsystem: Fagsystem): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.PÅBEGYNT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true),
                eksternFagsakId = eksternFagsakId,
                eksternBehandlingId = eksternBehandlingId,
                fagsystem = fagsystem
            )

        fun opprettVenterTask(behandlingId: UUID, eksternFagsakId: String, eksternBehandlingId: String, fagsystem: Fagsystem): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.VENTER,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(),
                eksternFagsakId = eksternFagsakId,
                eksternBehandlingId = eksternBehandlingId,
                fagsystem = fagsystem
            )

        fun opprettFerdigTask(behandlingId: UUID, eksternFagsakId: String, eksternBehandlingId: String, fagsystem: Fagsystem): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.FERDIG,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true),
                eksternFagsakId = eksternFagsakId,
                eksternBehandlingId = eksternBehandlingId,
                fagsystem = fagsystem
            )

        fun opprettSendtTilKATask(
            behandlingId: UUID,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String = SikkerhetContext.hentSaksbehandler(true),
            eksternFagsakId: String,
            eksternBehandlingId: String,
            fagsystem: Fagsystem
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.SENDT_TIL_KA,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = gjeldendeSaksbehandler,
                eksternFagsakId = eksternFagsakId,
                eksternBehandlingId = eksternBehandlingId,
                fagsystem = fagsystem
            )

        private fun opprettTask(
            behandlingId: UUID,
            hendelse: BehandlingsstatistikkHendelse,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String,
            eksternFagsakId: String,
            eksternBehandlingId: String,
            fagsystem: Fagsystem,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    BehandlingsstatistikkTaskPayload(
                        behandlingId,
                        hendelse,
                        hendelseTidspunkt,
                        gjeldendeSaksbehandler,
                    ),
                ),
                properties = Properties().apply {
                    this["saksbehandler"] = gjeldendeSaksbehandler
                    this["behandlingId"] = behandlingId.toString()
                    this["hendelse"] = hendelse.name
                    this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                    this["eksternFagsakId"] = eksternFagsakId
                    this["eksternBehandlingId"] = eksternBehandlingId
                    this["fagsystem"] = fagsystem.name
                },
            )

        const val TYPE = "behandlingsstatistikkKlageTask"
    }
}

data class BehandlingsstatistikkTaskPayload(
    val behandlingId: UUID,
    val hendelse: BehandlingsstatistikkHendelse,
    val hendelseTidspunkt: LocalDateTime,
    val gjeldendeSaksbehandler: String?,
)
