package no.nav.familie.klage.formkrav.domain

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Form(
    @Id
    val behandlingId: UUID,
    val fagsakId: UUID = UUID.randomUUID(),
    val klagePart: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageKonkret: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String = "begrunnelsen kommer her",
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}
