package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FormService(
    private val formRepository: FormRepository
) {


    fun opprettFormDto(behandlingId: UUID): FormDto{
        return formDto(behandlingId)
    }

    fun hentBehandling(behandlingId: UUID): Form = formRepository.findByIdOrThrow(behandlingId)
}