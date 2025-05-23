package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.OpprettRevurderingUtil.skalOppretteRevurderingAutomatisk
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.domain.tilFagsystemRevurdering
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.blankett.LagSaksbehandlingsblankettTask
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.distribusjon.SendTilKabalTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.HENLAGT
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.IKKE_MEDHOLD
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.IKKE_SATT
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat.MEDHOLD
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak.ORDINÆR
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
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
    private val taskService: TaskService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val brevService: BrevService,
    private val fagsystemVedtakService: FagsystemVedtakService,
    private val featureToggleService: FeatureToggleService,
    private val fagsakService: FagsakService,
) {
    /**
     * Skal ikke være @transactional fordi det er mulig å komme delvis igjennom løypa
     */
    @Transactional
    fun ferdigstillKlagebehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingsresultat = utledBehandlingResultat(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        validerKanFerdigstille(behandling)
        if (behandlingsresultat == IKKE_MEDHOLD || behandlingsresultat == IKKE_MEDHOLD_FORMKRAV_AVVIST) {
            when (behandling.årsak) {
                ORDINÆR -> {
                    brevService.lagBrevPdf(behandlingId)
                    opprettJournalførBrevTask(behandlingId, fagsak.eksternId, fagsak.fagsystem)
                }

                HENVENDELSE_FRA_KABAL -> {
                    opprettSendTilKabalTask(behandlingId, fagsak.eksternId, fagsak.fagsystem)
                }
            }
        }
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(
            behandlingId = behandling.id,
            eksternFagsakId = fagsak.eksternId,
            fagsystem = fagsak.fagsystem,
        )

        val opprettetRevurdering = opprettRevurderingHvisMedhold(behandling, behandlingsresultat)

        behandlingService.oppdaterBehandlingMedResultat(behandlingId, behandlingsresultat, opprettetRevurdering)
        stegService.oppdaterSteg(
            behandlingId,
            behandling.steg,
            stegForResultat(behandlingsresultat),
            behandlingsresultat,
        )
        taskService.save(
            LagSaksbehandlingsblankettTask.opprettTask(
                behandlingId = behandlingId,
                eksternFagsakId = fagsak.eksternId,
                fagsystem = fagsak.fagsystem,
            ),
        )
        if (behandlingsresultat == IKKE_MEDHOLD) {
            taskService.save(
                BehandlingsstatistikkTask.opprettSendtTilKATask(
                    behandlingId = behandlingId,
                    eksternFagsakId = fagsak.eksternId,
                    fagsystem = fagsak.fagsystem,
                ),
            )
        }
        taskService.save(
            BehandlingsstatistikkTask.opprettFerdigTask(
                behandlingId = behandlingId,
                eksternFagsakId = fagsak.eksternId,
                fagsak.fagsystem,
            ),
        )
    }

    /**
     * Oppretter revurdering automatisk ved medhold
     * Dette skjer synkront og kan vurderes å endres til async med task eller kafka ved behov
     */
    private fun opprettRevurderingHvisMedhold(
        behandling: Behandling,
        behandlingsresultat: BehandlingResultat,
    ): FagsystemRevurdering? =
        if (behandlingsresultat == MEDHOLD &&
            skalOppretteRevurderingAutomatisk(behandling.påklagetVedtak)
        ) {
            fagsystemVedtakService.opprettRevurdering(behandling).tilFagsystemRevurdering()
        } else {
            null
        }

    private fun opprettJournalførBrevTask(
        behandlingId: UUID,
        eksternFagsakId: String,
        fagsystem: Fagsystem,
    ) {
        val journalførBrevTask =
            Task(
                type = JournalførBrevTask.TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = SikkerhetContext.hentSaksbehandler(strict = true)
                        this["eksternFagsakId"] = eksternFagsakId
                        this["fagsystem"] = fagsystem.name
                    },
            )
        taskService.save(journalførBrevTask)
    }

    private fun opprettSendTilKabalTask(
        behandlingId: UUID,
        eksternFagsakId: String,
        fagsystem: Fagsystem,
    ) {
        val sendTilKabalTask =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = SikkerhetContext.hentSaksbehandler(strict = true)
                        this["eksternFagsakId"] = eksternFagsakId
                        this["fagsystem"] = fagsystem.name
                    },
            )
        taskService.save(sendTilKabalTask)
    }

    private fun stegForResultat(resultat: BehandlingResultat): StegType =
        when (resultat) {
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

    private fun utledBehandlingResultat(behandlingId: UUID): BehandlingResultat =
        if (formService.formkravErOppfyltForBehandling(behandlingId)) {
            vurderingService.hentVurdering(behandlingId)?.vedtak?.tilBehandlingResultat()
                ?: throw Feil("Burde funnet behandling $behandlingId")
        } else {
            IKKE_MEDHOLD_FORMKRAV_AVVIST
        }
}
