package no.nav.familie.klage.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.amelding.ekstern.AMeldingInntektClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class AInntektMock {

    @Profile("mock-inntekt")
    @Bean
    @Primary
    fun aMeldingInntektClient(): AMeldingInntektClient {
        val mockk = mockk<AMeldingInntektClient>()
        every { mockk.genererAInntektUrl(any()) } returns "https://familie-ef-proxy.dev-fss-pub.nais.io"
        return mockk
    }
}
