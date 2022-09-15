package no.nav.familie.klage.formkrav

import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository,
    private val stegService: StegService
) {

    fun hentForm(behandlingId: UUID): FormDto? {
        val eksisterer = formRepository.existsById(behandlingId)
        if (eksisterer) {
            val form = formRepository.findByIdOrThrow(behandlingId)
            return form.tilDto()
        }
        return null
    }

    @Transactional
    fun opprettEllerOppdaterForm(form: Form): FormDto {
        if (formkravErFerdigUtfyllt(form)) {
            if (formkravErOppfylt(form)) stegService.oppdaterSteg(form.behandlingId, StegType.VURDERING)
            else stegService.oppdaterSteg(form.behandlingId, StegType.BREV)
        } else {
            stegService.oppdaterSteg(form.behandlingId, StegType.FORMKRAV)
        }
        if (sjekkOmFormEksisterer(form.behandlingId)) {
            return oppdaterForm(form)
        }
        return formRepository.insert(
            Form(
                behandlingId = form.behandlingId,
                fagsakId = form.fagsakId,
                klagePart = form.klagePart,
                klageKonkret = form.klageKonkret,
                klagefristOverholdt = form.klagefristOverholdt,
                klageSignert = form.klageSignert,
                saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse,
            )
        ).tilDto()
    }

    private fun formkravErFerdigUtfyllt(form: Form) = !arrayOf(
        form.klageKonkret,
        form.klagePart,
        form.klageSignert,
        form.klagefristOverholdt
    ).contains(FormVilkår.IKKE_SATT) &&
        form.saksbehandlerBegrunnelse.isNotEmpty()

    @Transactional
    fun oppdaterForm(form: Form): FormDto {
        val formFraDb = formRepository.findByBehandlingId(form.behandlingId)
        return formRepository.update(
            formFraDb.copy(
                klagePart = form.klagePart,
                klagefristOverholdt = form.klagefristOverholdt,
                klageKonkret = form.klageKonkret,
                klageSignert = form.klageSignert,
                saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse
            )
        ).tilDto()
    }

    fun sjekkOmFormEksisterer(id: UUID): Boolean {
        return formRepository.findById(id).isPresent
    }

    fun formkravErOppfylt(form: Form): Boolean {
        return (
            form.klageKonkret == FormVilkår.OPPFYLT &&
                form.klagePart == FormVilkår.OPPFYLT &&
                form.klageSignert == FormVilkår.OPPFYLT &&
                form.klagefristOverholdt == FormVilkår.OPPFYLT &&
                form.saksbehandlerBegrunnelse != ""
            )
    }

    fun formkravErOppfyltForBehandling(behandlingId: UUID): Boolean {
        val form = formRepository.findByIdOrNull(behandlingId) ?: error("Fant ikke formkrav for behandling=$behandlingId")
        return formkravErOppfylt(form)
    }
}
