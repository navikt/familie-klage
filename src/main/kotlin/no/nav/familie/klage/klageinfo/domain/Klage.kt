package no.nav.familie.klage.klageinfo.domain

import org.springframework.data.annotation.Id
import java.time.LocalDate
import java.util.UUID

data class Klage(
    @Id
    val behandlingId: UUID,
    val fagsakId: UUID = UUID.randomUUID(),
    val vedtaksDato: LocalDate = LocalDate.now(),
    val klageMottatt: LocalDate = LocalDate.now(),
    val klageAarsak: String = "min klage",
    val klageBeskrivelse: String = "beskrivelse kommer her",
    val sakSistEndret: LocalDate = LocalDate.now()
)
