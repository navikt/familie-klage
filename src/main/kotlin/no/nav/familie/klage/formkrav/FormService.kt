package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository
) {
    fun hentForm(behandlingId: UUID): Form = formRepository.findByBehandlingId(behandlingId)

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
}