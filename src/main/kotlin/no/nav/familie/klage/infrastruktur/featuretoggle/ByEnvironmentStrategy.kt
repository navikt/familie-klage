package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByEnvironmentStrategy : Strategy {

    override fun getName(): String {
        return "byEnvironment"
    }

    override fun isEnabled(map: MutableMap<String, String>, context: UnleashContext): Boolean {
        return context.environment
            .map { env -> map[miljøKey]?.split(',')?.contains(env) ?: false }
            .orElse(false)
    }

    companion object {
        private const val miljøKey = "miljø"
    }
}
