package no.nav.familie.klage.vurdering.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.kabal.KabalHjemmel
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

enum class Hjemmel(val kabalHjemmel: KabalHjemmel) {
    FT_FEMTEN_TO(KabalHjemmel.FTRL_15_2),
    FT_FEMTEN_TRE(KabalHjemmel.FTRL_15_3),
    FT_FEMTEN_FIRE(KabalHjemmel.FTRL_15_4),
    FT_FEMTEN_FEM(KabalHjemmel.FTRL_15_5),
    FT_FEMTEN_SEKS(KabalHjemmel.FTRL_15_6),
    FT_FEMTEN_ÅTTE(KabalHjemmel.FTRL_15_8),
    FT_FEMTEN_NI(KabalHjemmel.FTRL_15_9),
    FT_FEMTEN_TI(KabalHjemmel.FTRL_15_10),
    FT_FEMTEN_ELLEVE(KabalHjemmel.FTRL_15_11),
    FT_FEMTEN_TOLV(KabalHjemmel.FTRL_15_12),
    FT_FEMTEN_TRETTEN(KabalHjemmel.FTRL_15_13),
    BT_TO(KabalHjemmel.BTRL_2),
    BT_FIRE(KabalHjemmel.BTRL_4),
    BT_FEM(KabalHjemmel.BTRL_5),
    BT_NI(KabalHjemmel.BTRL_9),
    BT_TRETTEN(KabalHjemmel.BTRL_13),
    FT_EØS(KabalHjemmel.EOES_AVTALEN),
    FT_EØS_FOR(KabalHjemmel.EOES_883_2004_6),
}
