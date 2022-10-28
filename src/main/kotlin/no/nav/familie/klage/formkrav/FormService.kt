package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.formkrav.FormUtil.formkravErFerdigUtfyllt
import no.nav.familie.klage.formkrav.FormUtil.formkravErOppfylt
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository,
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService
    private val taskRepository: TaskRepository
) {

    fun hentForm(behandlingId: UUID): Form = formRepository.findByIdOrThrow(behandlingId)

    @Transactional
    fun opprettInitielleFormkrav(behandlingId: UUID): Form {
        taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId=behandlingId))
        return formRepository.insert(Form(behandlingId = behandlingId))
    }

    @Transactional
    fun oppdaterForm(form: FormDto): FormDto {
        val behandlingId = form.behandlingId
        val nyttPåklagetVedtak = form.påklagetVedtak

        val oppdatertForm = formRepository.findByIdOrThrow(behandlingId).copy(
            klagePart = form.klagePart,
            klagefristOverholdt = form.klagefristOverholdt,
            klageKonkret = form.klageKonkret,
            klageSignert = form.klageSignert,
            saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse
        )
        behandlingService.oppdaterPåklagetVedtak(behandlingId, nyttPåklagetVedtak)
        if (formkravErFerdigUtfyllt(oppdatertForm, nyttPåklagetVedtak)) {
            if (formkravErOppfylt(oppdatertForm)) {
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.VURDERING)
            } else {
                vurderingService.slettVurderingForBehandling(behandlingId)
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.BREV)
            }
        } else {
            stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.FORMKRAV)
        }

        return formRepository.update(oppdatertForm).tilDto(nyttPåklagetVedtak)
    }

    fun formkravErOppfyltForBehandling(behandlingId: UUID): Boolean {
        val form = formRepository.findByIdOrThrow(behandlingId)
        return formkravErOppfylt(form)
    }

    fun hentFormDto(behandlingId: UUID): FormDto {
        val påklagetVedtak = behandlingService.hentBehandling(behandlingId).påklagetVedtak
        return hentForm(behandlingId).tilDto(påklagetVedtak.tilDto())
    }
}
