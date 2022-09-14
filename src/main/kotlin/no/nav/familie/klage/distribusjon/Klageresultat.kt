package no.nav.familie.klage.distribusjon

import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("klagerasultat")
data class Klageresultat(
    val behandlingId: UUID,
    val journalpostId: String? = null,
    val distribusjonsId: String? = null,
    val oversendtTilKabalTidspunkt: LocalDateTime? = null,
)
