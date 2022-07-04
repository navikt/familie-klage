package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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
        if(sjekkOmFormEksiterer(form.id)){
            return oppdaterForm(form)
        } else {
            return formRepository.insert(
                Form(
                    id = form.id,
                    fagsakId = form.fagsakId,
                    vedtaksdato = form.vedtaksdato,
                    klageMottat = LocalDateTime.now(),
                    klageaarsak = form.klageaarsak,
                    klageBeskrivelse = form.klageBeskrivelse,
                    klagePart = form.klagePart,
                    klageKonkret = form.klageKonkret,
                    klagefristOverholdt = form.klagefristOverholdt,
                    klageSignert = form.klageSignert,
                    saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse,
                    sakSistEndret = LocalDateTime.now()
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