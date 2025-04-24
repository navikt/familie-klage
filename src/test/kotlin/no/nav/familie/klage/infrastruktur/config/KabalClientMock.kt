package no.nav.familie.klage.infrastruktur.config

import io.mockk.Awaits
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.klage.kabal.KabalClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-kabal")
class KabalClientMock {
    @Bean
    @Primary
    fun kabalClient(): KabalClient {
        val kabalClient: KabalClient = mockk()
        every { kabalClient.sendTilKabal(any()) } just Awaits
        return kabalClient
    }
}
