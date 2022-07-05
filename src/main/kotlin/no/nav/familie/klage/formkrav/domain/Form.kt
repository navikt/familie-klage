package no.nav.familie.klage.formkrav.domain

import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Form(
    @Id
    val behandlingId: UUID,
    val fagsakId: UUID = UUID.randomUUID(),
    val vedtaksdato: LocalDateTime = LocalDateTime.now(),
    val klageMottat: LocalDateTime = LocalDateTime.now(),
    val klageaarsak: String = "min klage",
    val klageBeskrivelse: String = "beskrivelse kommer her",
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String = "begrunnelsen kommer her",
    val sakSistEndret: LocalDateTime = LocalDateTime.now()
)
enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}

