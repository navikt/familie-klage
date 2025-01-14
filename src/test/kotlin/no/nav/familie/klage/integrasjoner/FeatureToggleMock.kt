package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("mock-featuretoggle")
@Configuration
class FeatureToggleMock {
    @Bean
    @Primary
    fun featureToggleService(): FeatureToggleService {
        val mock = mockk<FeatureToggleService>()
        every { mock.isEnabled(any()) } returns true
        return mock
    }
}