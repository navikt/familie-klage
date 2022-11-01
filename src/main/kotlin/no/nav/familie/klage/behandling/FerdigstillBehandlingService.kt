package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.blankett.LagSaksbehandlingsblankettTask
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.HENLAGT
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.IKKE_MEDHOLD
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.IKKE_SATT
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.MEDHOLD
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
class FerdigstillBehandlingService(
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val formService: FormService,
    private val stegService: StegService,
    private val taskRepository: TaskRepository,
    private val oppgaveTaskService: OppgaveTaskService,
    private val brevService: BrevService
) {

    /**
     * Skal ikke være @transactional fordi det er mulig å komme delvis igjennom løypa
     */
    @Transactional
    fun ferdigstillKlagebehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingsresultat = utledBehandlingResultat(behandlingId)

        validerKanFerdigstille(behandling)
        if (behandlingsresultat != MEDHOLD) {
            brevService.lagBrevPdf(behandlingId)
            opprettJournalførBrevTask(behandlingId)
        }
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id)
        behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(behandlingId, behandlingsresultat)
        stegService.oppdaterSteg(behandlingId, behandling.steg, stegForResultat(behandlingsresultat), behandlingsresultat)
        taskRepository.save(LagSaksbehandlingsblankettTask.opprettTask(behandlingId))
        taskRepository.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = behandlingId))
    }

    private fun opprettJournalførBrevTask(behandlingId: UUID) {
        val journalførBrevTask = Task(
            type = JournalførBrevTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
            }
        )
        taskRepository.save(journalførBrevTask)
    }

    private fun stegForResultat(resultat: BehandlingResultat): StegType = when (resultat) {
        IKKE_MEDHOLD -> StegType.KABAL_VENTER_SVAR
        MEDHOLD, IKKE_MEDHOLD_FORMKRAV_AVVIST, HENLAGT -> StegType.BEHANDLING_FERDIGSTILT
        IKKE_SATT -> error("Kan ikke utlede neste steg når behandlingsresultatet er IKKE_SATT")
    }

    private fun validerKanFerdigstille(behandling: Behandling) {
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Kan ikke ferdigstille behandlingen da den er låst for videre behandling")
        }
        if (behandling.steg != StegType.BREV) {
            throw Feil("Kan ikke ferdigstille behandlingen fra steg=${behandling.steg}")
        }
    }

    private fun utledBehandlingResultat(behandlingId: UUID): BehandlingResultat {
        return if (formService.formkravErOppfyltForBehandling(behandlingId)) {
            vurderingService.hentVurdering(behandlingId)?.vedtak?.tilBehandlingResultat() ?: throw Feil("Burde funnet behandling $behandlingId")
        } else {
            IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
    }
}
