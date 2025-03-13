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
        unleashContext: UnleashContext
    ): Boolean {
        val currentUserId = unleashContext.userId.orElse(null)

        logger.info("UNLEASH_CONTEXT_DEBUG --- unleashContext sier at miljø er: ${unleashContext.environment}")
        logger.info("UNLEASH_CONTEXT_DEBUG --- unleashContext sier at unleashContext.userId er: $currentUserId.")
        logger.info("UNLEASH_CONTEXT_DEBUG --- map som sendes med har en størrelse på: ${map.size}.s")
        logger.info("UNLEASH_CONTEXT_DEBUG --- map som sendes med sine keys: ${map.keys} og verdier: ${map.values}.")

        return if (currentUserId != null) {
            logger.info("UNLEASH_CONTEXT_DEBUG --- currentUserId er ikke null, men: $currentUserId.")
            val mapVerdi = map[currentUserId]
            logger.info("UNLEASH_CONTEXT_DEBUG --- verdi for currentUserId i map er: $mapVerdi.")
            map[currentUserId]?.toBoolean() ?: false
        } else {
            false
        }
    }
}