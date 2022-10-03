package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.formkrav.FormUtil.formkravErFerdigUtfyllt
import no.nav.familie.klage.formkrav.FormUtil.formkravErOppfylt
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository,
    private val stegService: StegService
) {

    fun hentForm(behandlingId: UUID): Form = formRepository.findByIdOrThrow(behandlingId)

    @Transactional
    fun opprettInitielleFormkrav(behandlingId: UUID): Form {
        return formRepository.insert(Form(behandlingId = behandlingId))
    }

    @Transactional
    fun oppdaterForm(form: FormDto): FormDto {
        val behandlingId = form.behandlingId
        val oppdatertForm = formRepository.findByIdOrThrow(behandlingId).copy(
            klagePart = form.klagePart,
            klagefristOverholdt = form.klagefristOverholdt,
            klageKonkret = form.klageKonkret,
            klageSignert = form.klageSignert,
            saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse
        )
        if (formkravErFerdigUtfyllt(oppdatertForm)) {
            if (formkravErOppfylt(oppdatertForm)) {
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.VURDERING)
            } else {
                stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.BREV)
            }
        } else {
            stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV, StegType.FORMKRAV)
        }

        return formRepository.update(oppdatertForm).tilDto()
    }

    fun formkravErOppfyltForBehandling(behandlingId: UUID): Boolean {
        val form = formRepository.findByIdOrThrow(behandlingId)
        return formkravErOppfylt(form)
    }
}
