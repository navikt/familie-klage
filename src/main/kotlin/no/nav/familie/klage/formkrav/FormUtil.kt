package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.dto.FormDto
import java.time.LocalDateTime
import java.util.UUID

fun formDto(
    behandlingsId: UUID = UUID.randomUUID(),
    fagsakId: UUID = UUID.randomUUID(),
    vedtaksdato: LocalDateTime = LocalDateTime.now(), // TODO: endre til mulig nullverdi

    klageMottat: LocalDateTime = LocalDateTime.now().minusDays(10),
    klageÅrsak: String = "Fikk ikke nok penger",
    klageBeskrivelse: String = "jeg er sinna",

    klagePart: Boolean = true,
    klageKonkret: Boolean = true,
    klagefristOverholdt: Boolean = true,
    klageSignert: Boolean = true,

    saksbehandlerBegrunnelse: String = "OK HAN SKAL FÅ MER PEGNER",
    sakSistEndret: LocalDateTime = LocalDateTime.now().minusDays(1),

    fullført: Boolean = true

): FormDto =
    FormDto(
        behandlingsId,
        fagsakId,
        vedtaksdato,

        klageMottat,
        klageÅrsak,
        klageBeskrivelse,

        klagePart,
        klageKonkret,
        klagefristOverholdt,
        klageSignert,

        saksbehandlerBegrunnelse,
        sakSistEndret,

        fullført
    )