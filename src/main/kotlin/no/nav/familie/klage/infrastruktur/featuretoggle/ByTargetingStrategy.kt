package no.nav.familie.klage.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ByTargetingStrategy : Strategy {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun getName() = "flexibleRollout"

    override fun isEnabled(
        map: MutableMap<String, String>,
        unleashContext: UnleashContext,
    ): Boolean {
        val navIdent = unleashContext.userId.orElse(null)

        return if (navIdent != null) {
            map[navIdent]?.toBoolean() ?: false
        } else {
            false
        }
    }
}
