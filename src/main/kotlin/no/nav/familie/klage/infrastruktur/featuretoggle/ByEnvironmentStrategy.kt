package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByEnvironmentStrategy : Strategy {

    override fun getName(): String = "byEnvironment"

    override fun isEnabled(map: MutableMap<String, String>, unleashContext: UnleashContext): Boolean = unleashContext.environment
        .map { env -> map[MILJØ_KEY]?.split(',')?.contains(env) ?: false }
        .orElse(false)

    companion object {
        private const val MILJØ_KEY = "miljø"
    }
}
