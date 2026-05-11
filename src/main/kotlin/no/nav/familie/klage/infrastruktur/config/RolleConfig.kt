package no.nav.familie.klage.infrastruktur.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rolle")
class RolleConfig(
    val ba: FagsystemRolleConfig,
    val ef: FagsystemRolleConfig,
    val ks: FagsystemRolleConfig,
    val egenAnsatt: String,
) {
    val beslutterRoller = setOf(ba.beslutter, ks.beslutter, ef.beslutter)
    val saksbehandlerRoller = setOf(ba.saksbehandler, ks.saksbehandler, ef.saksbehandler)
    val veilederRoller = setOf(ba.veileder, ks.veileder, ef.veileder)
}

data class FagsystemRolleConfig(
    val saksbehandler: String,
    val beslutter: String,
    val veileder: String,
)
