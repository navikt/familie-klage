package no.nav.familie.klage.formkrav.dto

import no.nav.familie.klage.formkrav.domain.Form
import java.util.UUID

data class FormDto(
    val behandlingId: UUID,
    val fagsakId: UUID,
)

fun Form.tilDto(): FormDto =
    FormDto(
        behandlingId = this.behandlingId,
        fagsakId = this.fagsakId,
    )