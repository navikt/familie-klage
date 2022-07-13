package no.nav.familie.klage.brev.domain

import Fil
import no.nav.familie.klage.brev.dto.Avsnitt
import org.springframework.data.annotation.Id
import java.util.UUID

data class BrevMedAvsnitt(
    val behandlingId: UUID,
    val brevId: UUID,
    val overskrift: String,
    val avsnitt: List<Avsnitt>
    )

data class Brev(
    @Id
    val brevId: UUID,
    val behandlingId: UUID,
    val overskrift: String,
    val saksbehandlerHtml: String,
    val beslutterPdf: Fil? = null
)

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}
