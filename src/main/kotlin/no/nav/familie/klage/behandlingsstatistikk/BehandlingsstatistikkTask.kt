package no.nav.familie.klage.behandlingsstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
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
    settTilManuellOppfølgning = true
)
class BehandlingsstatistikkTask(
    private val behandlingStatistikkService: BehandlingsstatistikkService,
    private val featureToggleService: FeatureToggleService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        feilHvisIkke(featureToggleService.isEnabled(Toggle.BEHANDLINGSSTATISTIKK)) {
            "Funksjonen for sending av behandlingsstatistikk er slått av"
        }
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler) =
            objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)
        behandlingStatistikkService.sendBehandlingstatistikk(
            behandlingId,
            hendelse,
            hendelseTidspunkt,
            gjeldendeSaksbehandler
        )
    }

    companion object {

        fun opprettMottattTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.MOTTATT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler()
            )

        fun opprettPåbegyntTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.PÅBEGYNT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true)
            )

        fun opprettFerdigTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.FERDIG,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true)
            )

        fun opprettSendtTilKATask(
            behandlingId: UUID,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String = SikkerhetContext.hentSaksbehandler(true)
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = BehandlingsstatistikkHendelse.SENDT_TIL_KA,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = gjeldendeSaksbehandler
            )

        private fun opprettTask(
            behandlingId: UUID,
            hendelse: BehandlingsstatistikkHendelse,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    BehandlingsstatistikkTaskPayload(
                        behandlingId,
                        hendelse,
                        hendelseTidspunkt,
                        gjeldendeSaksbehandler
                    )
                ),
                properties = Properties().apply {
                    this["saksbehandler"] = gjeldendeSaksbehandler ?: ""
                    this["behandlingId"] = behandlingId.toString()
                    this["hendelse"] = hendelse.name
                    this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                }
            )

        const val TYPE = "behandlingsstatistikkKlageTask"
    }
}

data class BehandlingsstatistikkTaskPayload(
    val behandlingId: UUID,
    val hendelse: BehandlingsstatistikkHendelse,
    val hendelseTidspunkt: LocalDateTime,
    val gjeldendeSaksbehandler: String?
)
