package no.nav.familie.klage.formkrav


import no.nav.familie.klage.formkrav.dto.FormDto
import java.time.LocalDate
import java.util.UUID

fun formDto(
    behandlingId: UUID = UUID.randomUUID(),
    fagsakId: UUID = UUID.randomUUID(),
    vedtaksdato: LocalDate = LocalDate.now(), // TODO: endre til mulig nullverdi
    klageMottat: LocalDate = LocalDate.now().minusDays(10),
    klageaarsak: String = "Fikk ikke nok penger",
    klageBeskrivelse: String = "jeg er sinna",
    sakSistEndret: LocalDate = LocalDate.now().minusDays(1)
): FormDto =
    FormDto(
        behandlingId,
        fagsakId,
        vedtaksdato,
        klageMottat,
        klageaarsak,
        klageBeskrivelse,
        sakSistEndret,
    )