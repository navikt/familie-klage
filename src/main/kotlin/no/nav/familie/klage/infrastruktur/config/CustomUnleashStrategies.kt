package no.nav.familie.klage.infrastruktur.config

import io.getunleash.strategy.Strategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomUnleashStrategies {

    @Bean
    fun strategies(): List<Strategy> {
        // TODO: Husk å enable igjen. Kommentert ut for å teste om strategier faktisk blir brukt.
        // return listOf(ByEnvironmentStrategy(), ByTargetingStrategy())
        return emptyList()
    }
}
