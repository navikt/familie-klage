package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository
) {

    fun opprettFormDto(behandlingId: UUID): FormDto {
        return formDto(behandlingId)
    }

    fun opprettForm(form: Form): Form {
        return formRepository.insert(
            Form(
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