package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext.hentClaimFraToken
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.sikkerhet.UgyldigJwtTokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProsesseringInfoProviderConfig {
    @Bean
    fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String =
            hentClaimFraToken("preferred_username")
                ?: throw UgyldigJwtTokenException("Fant ikke preferred_username i token")

        override fun harTilgang(): Boolean = SikkerhetContext.harRolle(prosesseringRolle)
    }
}
