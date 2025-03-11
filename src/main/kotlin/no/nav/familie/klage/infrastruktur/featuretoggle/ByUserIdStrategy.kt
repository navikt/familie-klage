package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByUserIdStrategy : Strategy {

    override fun getName(): String {
        return "byUserId"
    }

    override fun isEnabled(map: MutableMap<String, String>, context: UnleashContext): Boolean {
        return context.userId
            .map { userId -> map["user"]?.split(',')?.contains(userId) ?: false }
            .orElse(false)
    }
}
