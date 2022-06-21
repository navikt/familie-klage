package no.nav.familie.ef.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brev.BrevClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-brev")
class BrevClientMock {

    @Bean
    @Primary
    fun brevClient(): BrevClient {
        val brevClient: BrevClient = mockk()
        every { brevClient.genererHtmlFritekstbrev(any(), any(), any()) } returns "<h1>Hei BESLUTTER_SIGNATUR</h1>"
        return brevClient
    }
}
