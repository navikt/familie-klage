package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByUserIdStrategy : Strategy {

    override fun getName(): String = "byUserId"

    override fun isEnabled(map: MutableMap<String, String>, unleashContext: UnleashContext): Boolean =
        unleashContext.userId
            .map { userId -> map["user"]?.split(',')?.contains(userId) ?: false }
            .orElse(false)
}
