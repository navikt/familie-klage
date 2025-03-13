package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByTargetingStrategy : Strategy {
    override fun getName() = "flexibleRollout"

    override fun isEnabled(
        map: MutableMap<String, String>,
        unleashContext: UnleashContext
    ): Boolean {
        val currentUserId = unleashContext.userId.orElse(null)

        return if (currentUserId != null) {
            map[currentUserId]?.toBoolean() ?: false
        } else {
            false
        }
    }

}