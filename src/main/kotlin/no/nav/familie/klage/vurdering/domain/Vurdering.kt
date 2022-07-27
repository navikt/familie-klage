package no.nav.familie.klage.vurdering.domain

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vurdering(
    @Id
    val behandlingId: UUID,
    val vedtak: Vedtak,
    val arsak: Arsak? = null,
    val hjemmel: Hjemmel? = null,
    val beskrivelse: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

enum class Vedtak {
    VELG,
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK,
}

enum class Arsak {
    VELG,
    SAKSBEHANDLINGSFEIL,
    TODO1,
    TODO2,
}

enum class Hjemmel {
    VELG,
    FEMTEN_TO,
    FEMTEN_TRE,
    FEMTEN_FIRE,
}