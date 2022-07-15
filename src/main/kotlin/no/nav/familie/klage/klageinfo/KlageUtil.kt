package no.nav.familie.klage.klageinfo

import no.nav.familie.klage.klageinfo.domain.Klage
import java.time.LocalDate
import java.util.UUID

fun klageMock(
    behandlingId: UUID,
    fagsakId: UUID = UUID.randomUUID(),
    vedtaksDato: LocalDate = LocalDate.now(), // TODO: endre til mulig nullverdi
    klageMottatt: LocalDate = LocalDate.now().minusDays(10),
    klageAarsak: String = "Fikk ikke nok penger",
    klageBeskrivelse: String = "jeg er sinna",
    sakSistEndret: LocalDate = LocalDate.now().minusDays(1)
): Klage =
    Klage(
        behandlingId,
        fagsakId,
        vedtaksDato,
        klageMottatt,
        klageAarsak,
        klageBeskrivelse,
        sakSistEndret,
    )