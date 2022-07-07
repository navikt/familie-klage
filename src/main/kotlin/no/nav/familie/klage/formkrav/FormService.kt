package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository
) {
    fun hentForm(id: UUID): Form = formRepository.findByIdOrThrow(id)

    fun opprettFormDto(): FormDto {
        return formDto()
    }

    fun opprettForm(form: Form): Form {
        println("oppretter form: " + form)
        if(sjekkOmFormEksiterer(form.behandlingId)){
            return oppdaterForm(form)
        } else {
            return formRepository.insert(
                Form(
                    behandlingId = form.behandlingId,
                    fagsakId = form.fagsakId,
                    vedtaksdato = form.vedtaksdato,
                    klageMottat = LocalDate.now(),
                    klageaarsak = form.klageaarsak,
                    klageBeskrivelse = form.klageBeskrivelse,
                    klagePart = form.klagePart,
                    klageKonkret = form.klageKonkret,
                    klagefristOverholdt = form.klagefristOverholdt,
                    klageSignert = form.klageSignert,
                    saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse,
                    sakSistEndret = LocalDate.now()
                )
            )
        }
    }

    fun oppdaterForm(form: Form): Form {
        return formRepository.update(form.copy())
    }

    fun sjekkOmFormEksiterer(id: UUID): Boolean{
        return formRepository.findById(id).isPresent
    }
}