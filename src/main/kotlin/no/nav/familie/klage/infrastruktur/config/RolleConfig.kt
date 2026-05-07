package no.nav.familie.klage.infrastruktur.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rolle")
class RolleConfig(
    val ba: FagsystemRolleConfig,
    val ef: FagsystemRolleConfig,
    val ks: FagsystemRolleConfig,
    val egenAnsatt: String,
) {
    val beslutterRoller: Set<String>
        get() = setOf(ba.beslutter, ef.beslutter, ks.beslutter)

    val saksbehandlerRoller: Set<String>
        get() = setOf(ba.saksbehandler, ks.saksbehandler, egenAnsatt)

    val veilederRoller: Set<String>
        get() = setOf(ba.veileder, ks.veileder, ef.veileder)
}

data class FagsystemRolleConfig(
    val saksbehandler: String,
    val beslutter: String,
    val veileder: String,
)
