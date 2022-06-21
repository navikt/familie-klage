package no.nav.familie.klage.infrastruktur.config

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.UnleashContextProvider
import no.finn.unleash.util.UnleashConfig
import no.nav.familie.klage.infrastruktur.featuretoggle.ByEnvironmentStrategy
import no.nav.familie.klage.infrastruktur.featuretoggle.ByUserIdStrategy
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import java.net.URI

@ConfigurationProperties("funksjonsbrytere")
@ConstructorBinding
class FeatureToggleConfig(
    private val enabled: Boolean,
    private val unleash: Unleash
) {

    @ConstructorBinding
    data class Unleash(
        val uri: URI,
        val environment: String,
        val applicationName: String
    )

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun featureToggle(): FeatureToggleService =
        if (enabled)
            lagUnleashFeatureToggleService()
        else {
            log.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
            )
            lagDummyFeatureToggleService()
        }

    private fun lagUnleashFeatureToggleService(): FeatureToggleService {
        val unleash = DefaultUnleash(
                UnleashConfig.builder()
                .appName(unleash.applicationName)
                .unleashAPI(unleash.uri)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
                ByEnvironmentStrategy(), ByUserIdStrategy()
        )

        return object : FeatureToggleService {
            override fun isEnabled(toggle: Toggle, defaultValue: Boolean): Boolean {
                return unleash.isEnabled(toggle.toggleId, defaultValue)
            }

            // Spring trigger denne ved shutdown. Gjøres for å unngå at unleash fortsetter å gjøre kall ut
            override fun destroy() {
                unleash.shutdown()
            }
        }
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                // .userId("a user") // Må legges til en gang i fremtiden
                .environment(unleash.environment)
                .appName(unleash.applicationName)
                .build()
        }
    }

    private fun lagDummyFeatureToggleService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun isEnabled(toggle: Toggle, defaultValue: Boolean): Boolean {
                if (unleash.environment == "local") {
                    return true
                }
                return defaultValue
            }

            override fun destroy() {
                // Dummy featureToggleService trenger ikke destroy, då den ikke har en unleash å lukke
            }
        }
    }
}
