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
    fun hentForm(behandlingId: UUID): Form = formRepository.findByIdOrThrow(behandlingId)

    fun hentMockFormDto(behandlingId: UUID): FormDto{
        return formDto()
    }

    fun opprettForm(form: Form): Form {
        if(sjekkOmFormEksiterer(form.behandlingId)){
            return oppdaterForm(form)
        } else {
            return formRepository.insert(
                Form(
                    behandlingId = form.behandlingId,
                    fagsakId = form.fagsakId,
                    vedtaksdato = form.vedtaksdato,
                    klageMottatt = form.klageMottatt,
                    klageaarsak = form.klageaarsak,
                    klageBeskrivelse = form.klageBeskrivelse,
                    klagePart = form.klagePart,
                    klageKonkret = form.klageKonkret,
                    klagefristOverholdt = form.klagefristOverholdt,
                    klageSignert = form.klageSignert,
                    saksbehandlerBegrunnelse = form.saksbehandlerBegrunnelse,
                    sakSistEndret = form.sakSistEndret
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