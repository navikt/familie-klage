package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
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
            toggleId = featureToggle.navn,
            properties = toggleUnleashContextFields,
        )
    }
}

sealed class FeatureToggle(
    val navn: String,
) {
    data object TestToggleMedStrategi : FeatureToggle(navn = "familie-klage.test-toggle-med-strategi")
}

internal fun FeatureToggle.mapUnleashContextFields(): Map<String, String> {
    return when (this) {
        is FeatureToggle.TestToggleMedStrategi -> mapOf(
            UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
        )
    }
}
