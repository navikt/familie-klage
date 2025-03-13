package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UnleashNextService(
    private val unleashService: UnleashService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun isEnabled(featureToggle: FeatureToggle): Boolean {
        val toggleUnleashContextFields = featureToggle.mapUnleashContextFields()

        return unleashService.isEnabled(
            toggleId = featureToggle.toggleId,
            properties = toggleUnleashContextFields,
        )
    }
}
