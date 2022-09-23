package no.nav.familie.klage.infrastruktur.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("rolle")
@ConstructorBinding
class RolleConfig(
    val ba: FagsystemRolleConfig,
    val ef: FagsystemRolleConfig,
    val ks: FagsystemRolleConfig
)

data class FagsystemRolleConfig(
    val saksbehandler: String,
    val beslutter: String,
    val veileder: String
)
