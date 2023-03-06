package no.nav.familie.klage.infrastruktur.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rolle")
class RolleConfig(
    val ba: FagsystemRolleConfig,
    val ef: FagsystemRolleConfig,
    val ks: FagsystemRolleConfig,
)

data class FagsystemRolleConfig(
    val saksbehandler: String,
    val beslutter: String,
    val veileder: String,
)
