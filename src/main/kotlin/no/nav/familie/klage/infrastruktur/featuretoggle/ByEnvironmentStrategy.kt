package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByEnvironmentStrategy : Strategy {
    override fun getName(): String = "byEnvironment"

    override fun isEnabled(
        map: MutableMap<String, String>,
        unleashContext: UnleashContext,
    ): Boolean {
        val miljø = unleashContext.environment.orElse(null)

        return if (miljø != null) {
            map[miljø]?.toBoolean() ?: false
        } else {
            false
        }
    }
}
