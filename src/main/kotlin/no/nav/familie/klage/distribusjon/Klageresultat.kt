package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.util.UUID

data class Klageresultat(
    @Id
    val behandlingId: UUID,
    val journalpostId: String? = null,
    val distribusjonId: String? = null,
    val oversendtTilKabalTidspunkt: LocalDateTime? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)
