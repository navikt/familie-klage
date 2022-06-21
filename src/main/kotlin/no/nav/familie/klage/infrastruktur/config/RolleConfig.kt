package no.nav.familie.klage.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
    @Value("\${rolle.beslutter}")
    val beslutterRolle: String,
    @Value("\${rolle.saksbehandler}")
    val saksbehandlerRolle: String,
    @Value("\${rolle.veileder}")
    val veilederRolle: String,
    @Value("\${rolle.kode6}")
    val kode6: String,
    @Value("\${rolle.kode7}")
    val kode7: String
)
