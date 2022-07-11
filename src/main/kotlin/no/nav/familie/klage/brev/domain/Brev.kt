package no.nav.familie.klage.brev.domain

import org.springframework.data.annotation.Id
import java.util.UUID

data class Brev(
    @Id
    val behandlingId: UUID,
    val fagsakId: UUID = UUID.randomUUID(),
    val klagePart: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageKonkret: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String = "begrunnelsen kommer her",
)

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}
