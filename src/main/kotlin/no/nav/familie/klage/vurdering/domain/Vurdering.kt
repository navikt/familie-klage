package no.nav.familie.klage.vurdering.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.kabal.KabalHjemmel
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Årsak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vurdering(
    @Id
    val behandlingId: UUID,
    val vedtak: Vedtak,
    @Column("arsak")
    val årsak: Årsak? = null,
    @Column("begrunnelse_omgjoring")
    val begrunnelseOmgjøring: String? = null,
    val hjemmel: Hjemmel? = null,
    val innstillingKlageinstans: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val interntNotat: String?
)

enum class Vedtak {
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK;

    fun tilBehandlingResultat(): BehandlingResultat {
        return when (this) {
            OMGJØR_VEDTAK -> BehandlingResultat.MEDHOLD
            OPPRETTHOLD_VEDTAK -> BehandlingResultat.IKKE_MEDHOLD
        }
    }
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
    FT_TJUETO_FEMTEN(KabalHjemmel.FTRL_22_15),
    BT_TO(KabalHjemmel.BTRL_2),
    BT_FIRE(KabalHjemmel.BTRL_4),
    BT_FEM(KabalHjemmel.BTRL_5),
    BT_NI(KabalHjemmel.BTRL_9),
    BT_TI(KabalHjemmel.BTRL_10),
    BT_ELLEVE(KabalHjemmel.BTRL_11),
    BT_TOLV(KabalHjemmel.BTRL_12),
    BT_TRETTEN(KabalHjemmel.BTRL_13),
    BT_SYTTEN(KabalHjemmel.BTRL_17),
    BT_ATTEN(KabalHjemmel.BTRL_18),
    KS_TO(KabalHjemmel.KONTSL_2),
    KS_TRE(KabalHjemmel.KONTSL_3),
    KS_SEKS(KabalHjemmel.KONTSL_6),
    KS_SYV(KabalHjemmel.KONTSL_7),
    KS_ÅTTE(KabalHjemmel.KONTSL_8),
    KS_NI(KabalHjemmel.KONTSL_9),
    KS_TI(KabalHjemmel.KONTSL_10),
    KS_ELLEVE(KabalHjemmel.KONTSL_11),
    KS_TOLV(KabalHjemmel.KONTSL_12),
    KS_TRETTEN(KabalHjemmel.KONTSL_13),
    KS_SEKSTEN(KabalHjemmel.KONTSL_16),
    FT_EØS(KabalHjemmel.EOES_AVTALEN),
    NORDISK(KabalHjemmel.NORDISK_KONVENSJON),
    TA(KabalHjemmel.ANDRE_TRYGDEAVTALER),
    FT_EØS_FOR_FEM(KabalHjemmel.EOES_883_2004_5),
    FT_EØS_FOR_SEKS(KabalHjemmel.EOES_883_2004_6)
}
