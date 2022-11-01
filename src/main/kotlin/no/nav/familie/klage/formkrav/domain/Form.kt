package no.nav.familie.klage.formkrav.domain

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Form(
    @Id
    val behandlingId: UUID,
    val klagePart: FormVilkår = FormVilkår.IKKE_SATT,
    val klagefristOverholdt: FormVilkår = FormVilkår.IKKE_SATT,
    val klageKonkret: FormVilkår = FormVilkår.IKKE_SATT,
    val klageSignert: FormVilkår = FormVilkår.IKKE_SATT,
    val saksbehandlerBegrunnelse: String = "",
    val brevtekst: String = "",
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}
