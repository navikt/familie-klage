package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
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

    fun opprettForm(fagsakId: UUID): Form {
        return formRepository.insert(
            Form(
                fagsakId = fagsakId,
                vedtaksdato = LocalDateTime.now(),
                klageMottat = LocalDateTime.now(),
                klageaarsak = "min klage",
                klageBeskrivelse = "min klagebeskrivelse",
                klagePart = FormVilkår.IKKE_OPPFYLT,
                klageKonkret = FormVilkår.IKKE_OPPFYLT,
                klagefristOverholdt = FormVilkår.IKKE_OPPFYLT,
                klageSignert = FormVilkår.IKKE_OPPFYLT,
                saksbehandlerBegrunnelse = "det er greit",
                sakSistEndret = LocalDateTime.now(),
                vilkaarStatus = FormVilkår.IKKE_OPPFYLT
            )
        )
    }
}