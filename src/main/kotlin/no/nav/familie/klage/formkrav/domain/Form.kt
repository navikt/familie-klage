package no.nav.familie.klage.formkrav.domain

import org.springframework.data.annotation.Id
import java.time.LocalDate
import java.util.UUID

data class Form(
    @Id
    val behandlingId: UUID,
    val fagsakId: UUID = UUID.randomUUID(),
    val vedtaksdato: LocalDate = LocalDate.now(),
    val klageMottat: LocalDate = LocalDate.now(),
    val klageaarsak: String = "min klage",
    val klageBeskrivelse: String = "beskrivelse kommer her",
    val klagePart: FormVilkår,
    val klageKonkret: FormVilkår,
    val klagefristOverholdt: FormVilkår,
    val klageSignert: FormVilkår,
    val saksbehandlerBegrunnelse: String = "begrunnelsen kommer her",
    val sakSistEndret: LocalDate = LocalDate.now()
)
enum class FormVilkår {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_SATT
}

