package no.nav.familie.klage.institusjon

import org.springframework.data.annotation.Id
import java.util.UUID

data class Institusjon(
    @Id
    val id: UUID = UUID.randomUUID(),
    val orgNummer: String,
    val navn: String,
    val tssEksternId: String?,
)
