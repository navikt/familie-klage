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
    val dokumentasjonOgUtredning: String? = null,
    @Column("sporsmalet_i_saken")
    val spørsmåletISaken: String? = null,
    val aktuelleRettskilder: String? = null,
    @Column("klagers_anforsler")
    val klagersAnførsler: String? = null,
    val vurderingAvKlagen: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val interntNotat: String?,
)

enum class Vedtak {
    OMGJØR_VEDTAK,
    OPPRETTHOLD_VEDTAK,
    ;

    fun tilBehandlingResultat(): BehandlingResultat =
        when (this) {
            OMGJØR_VEDTAK -> BehandlingResultat.MEDHOLD
            OPPRETTHOLD_VEDTAK -> BehandlingResultat.IKKE_MEDHOLD
        }
}

@Suppress("unused")
enum class Hjemmel(
    val kabalHjemmel: KabalHjemmel,
) {
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
    FT_TJUETO_TOLV(KabalHjemmel.FTRL_22_12),
    FT_TJUETO_TRETTEN(KabalHjemmel.FTRL_22_13),
    FT_TJUETO_FEMTEN(KabalHjemmel.FTRL_22_15),
    BT_TO(KabalHjemmel.BTRL_2),
    BT_FIRE(KabalHjemmel.BTRL_4),
    BT_FEM(KabalHjemmel.BTRL_5),
    BT_NI(KabalHjemmel.BTRL_9),
    BT_TI(KabalHjemmel.BTRL_10),
    BT_ELLEVE(KabalHjemmel.BTRL_11),
    BT_TOLV(KabalHjemmel.BTRL_12),
    BT_TRETTEN(KabalHjemmel.BTRL_13),
    BT_FJORTEN(KabalHjemmel.BTRL_14),
    BT_FEMTEN(KabalHjemmel.BTRL_15),
    BT_SYTTEN(KabalHjemmel.BTRL_17),
    BT_ATTEN(KabalHjemmel.BTRL_18),
    KS_EN_A(KabalHjemmel.KONTSL_1A),
    KS_TO(KabalHjemmel.KONTSL_2),
    KS_TRE(KabalHjemmel.KONTSL_3),
    KS_TRE_A(KabalHjemmel.KONTSL_3A),
    KS_FIRE(KabalHjemmel.KONTSL_4),
    KS_SEKS(KabalHjemmel.KONTSL_6),
    KS_SYV(KabalHjemmel.KONTSL_7),
    KS_ÅTTE(KabalHjemmel.KONTSL_8),
    KS_NI(KabalHjemmel.KONTSL_9),
    KS_TI(KabalHjemmel.KONTSL_10),
    KS_ELLEVE(KabalHjemmel.KONTSL_11),
    KS_TOLV(KabalHjemmel.KONTSL_12),
    KS_TRETTEN(KabalHjemmel.KONTSL_13),
    KS_SEKSTEN(KabalHjemmel.KONTSL_16),
    KS_FJORTEN(KabalHjemmel.KONTSL_14),
    KS_SYTTEN(KabalHjemmel.KONTSL_17),
    KS_NITTEN(KabalHjemmel.KONTSL_19),
    KS_TJUETO(KabalHjemmel.KONTSL_22),
    FV_TJUEÅTTE(KabalHjemmel.FVL_28),
    FV_TJUENI(KabalHjemmel.FVL_29),
    FV_TRETTI(KabalHjemmel.FVL_30),
    FV_TRETTIEN(KabalHjemmel.FVL_31),
    FV_TRETTITO(KabalHjemmel.FVL_32),
    FV_TRETTITRE(KabalHjemmel.FVL_33),
    FV_TRETTIFIRE(KabalHjemmel.FVL_34),
    FV_TRETTIFEM(KabalHjemmel.FVL_35),
    FV_TRETTISEKS(KabalHjemmel.FVL_36),
    UTLAND_EØS(KabalHjemmel.EOES_AVTALEN),
    UTLAND_NORDISK(KabalHjemmel.NORDISK_KONVENSJON),
    UTLAND_TRYGDEAVTALER(KabalHjemmel.ANDRE_TRYGDEAVTALER),
}
