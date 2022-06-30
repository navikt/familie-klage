package no.nav.familie.klage.formkrav.domain

import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Form(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val vedtaksdato: LocalDateTime,
    val klageMottat: LocalDateTime,
    val klageaarsak: String,
    val klageBeskrivelse: String,
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String,
    val sakSistEndret: LocalDateTime,
    val vilkaarStatus: FormVilkår
)

enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT
}

