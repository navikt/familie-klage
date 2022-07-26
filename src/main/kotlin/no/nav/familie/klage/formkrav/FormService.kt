package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository
) {
    fun hentForm(behandlingId: UUID): FormDto{
        val form = formRepository.findByIdOrThrow(behandlingId)
        return form.tilDto()
    }

    fun opprettForm(form: Form): Form {
        if(sjekkOmFormEksiterer(form.behandlingId)){
            return oppdaterForm(form)
        } else {
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
            )
        }
    }

    fun oppdaterForm(form: Form): Form {
        val formFraDb = formRepository.findByBehandlingId(form.behandlingId)
        return formRepository.update(formFraDb.copy(
            klagePart = form.klagePart,
            klagefristOverholdt = form.klagefristOverholdt,
            klageKonkret = form.klageKonkret,
            klageSignert = form.klageSignert,
            saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse
        ))
    }

    fun sjekkOmFormEksiterer(id: UUID): Boolean{
        return formRepository.findById(id).isPresent
    }

    fun formkravErOppfylt(behandlingId: UUID): Boolean{
        val form = formRepository.findByIdOrThrow(behandlingId)
        return(
            form.klageKonkret == FormVilkår.OPPFYLT &&
            form.klagePart == FormVilkår.OPPFYLT &&
            form.klageSignert == FormVilkår.OPPFYLT &&
            form.klagefristOverholdt == FormVilkår.OPPFYLT &&
            form.saksbehandlerBegrunnelse != "")
    }
}