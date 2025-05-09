package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormUtil.alleVilkårOppfylt
import no.nav.familie.klage.formkrav.FormUtil.utledFormresultat
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.FormkravDto
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository,
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val vurderingService: VurderingService,
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
) {
    fun hentForm(behandlingId: UUID): Form = formRepository.findByIdOrThrow(behandlingId)

    @Transactional
    fun opprettInitielleFormkrav(behandlingId: UUID): Form = formRepository.insert(Form(behandlingId = behandlingId))

    @Transactional
    fun oppdaterFormkrav(formkrav: FormkravDto): FormkravDto {
        val behandlingId = formkrav.behandlingId
        val nyttPåklagetVedtak = formkrav.påklagetVedtak

        val oppdaterteFormkrav =
            formRepository.findByIdOrThrow(behandlingId).copy(
                klagePart = formkrav.klagePart,
                klagefristOverholdt = formkrav.klagefristOverholdt,
                klagefristOverholdtUnntak = formkrav.klagefristOverholdtUnntak,
                klageKonkret = formkrav.klageKonkret,
                klageSignert = formkrav.klageSignert,
                saksbehandlerBegrunnelse = formkrav.saksbehandlerBegrunnelse,
                brevtekst = formkrav.brevtekst,
            )
        behandlingService.oppdaterPåklagetVedtak(behandlingId, nyttPåklagetVedtak)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        opprettBehandlingsstatistikk(behandlingId, fagsak.eksternId, fagsak.fagsystem)
        val formresultat = utledFormresultat(oppdaterteFormkrav, nyttPåklagetVedtak)
        when (formresultat) {
            FormVilkår.OPPFYLT -> {
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.VURDERING)
            }

            FormVilkår.IKKE_OPPFYLT -> {
                vurderingService.slettVurderingForBehandling(behandlingId)
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.BREV)
            }

            FormVilkår.IKKE_SATT -> {
                vurderingService.slettVurderingForBehandling(behandlingId)
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.FORMKRAV)
            }
        }
        return formRepository.update(oppdaterteFormkrav).tilDto(nyttPåklagetVedtak)
    }

    private fun opprettBehandlingsstatistikk(
        behandlingId: UUID,
        eksternFagsakId: String,
        fagsystem: Fagsystem,
    ) {
        behandlingshistorikkService.hentBehandlingshistorikk(behandlingId).find { it.steg == StegType.FORMKRAV }
            ?: run {
                taskService.save(
                    BehandlingsstatistikkTask.opprettPåbegyntTask(
                        behandlingId = behandlingId,
                        eksternFagsakId = eksternFagsakId,
                        fagsystem = fagsystem,
                    ),
                )
            }
    }

    fun formkravErOppfyltForBehandling(behandlingId: UUID): Boolean {
        val form = formRepository.findByIdOrThrow(behandlingId)
        return alleVilkårOppfylt(form)
    }

    fun hentFormDto(behandlingId: UUID): FormkravDto {
        val påklagetVedtak = behandlingService.hentBehandling(behandlingId).påklagetVedtak
        return hentForm(behandlingId).tilDto(påklagetVedtak.tilDto())
    }
}
