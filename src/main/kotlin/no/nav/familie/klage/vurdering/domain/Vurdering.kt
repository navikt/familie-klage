package no.nav.familie.klage.vurdering.domain

import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vurdering(
        @Id
    val behandlingId: UUID,
        val vedtak: Vedtak,
        val arsak: Årsak? = null,
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

enum class Årsak {
    VELG,
    FEIL_I_LOVANDVENDELSE,
    FEIL_REGELVERKSFORSTÅELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    FEIL_PROSESSUELL,
    KØET_BEHANDLING,
    ANNET
}

enum class Hjemmel {
    VELG,
    FEMTEN_TO,
    FEMTEN_TRE,
    FEMTEN_FIRE,
    FEMTEN_FEM,
    FEMTEN_SEKS,
    FEMTEN_ÅTTE,
    FEMTEN_NI,
    FEMTEN_TI,
    FEMTEN_ELLEVE,
    FEMTEN_TOLV,
    FEMTEN_TRETTEN,
    TO,
    FIRE,
    FEM,
    NI,
    TRETTEN,
    EØS
}
