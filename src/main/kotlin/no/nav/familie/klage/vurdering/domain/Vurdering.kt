package no.nav.familie.klage.vurdering.domain

import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Vurdering(
    @Id
    val behandlingId: UUID = UUID.randomUUID(),
    val vedtak: Vedtak,
    val arsak: Årsak? = null,
    val hjemmel: Hjemmel? = null,
    val beskrivelse: String,
    val fullfortDato: LocalDateTime? = LocalDateTime.now()
)

enum class Vedtak {
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK,
}

enum class Årsak {
    SAKSBEHANDLINGSFEIL,
    TODO1,
    TODO2,
}

enum class Hjemmel {
    FEMTEN_TO,
    FEMTEN_TRE,
    FEMTEN_FIRE,
}