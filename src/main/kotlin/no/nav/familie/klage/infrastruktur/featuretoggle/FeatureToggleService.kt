package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.familie.unleash.DefaultUnleashService
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(val defaultUnleashService: DefaultUnleashService) {

    fun isEnabled(toggle: Toggle): Boolean {
        return defaultUnleashService.isEnabled(toggle.toggleId, false)
    }

    fun isEnabled(toggle: Toggle, defaultVerdi: Boolean): Boolean {
        return defaultUnleashService.isEnabled(toggle.toggleId, defaultVerdi)
    }
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    PLACEHOLDER("Ktlint liker ikke tomme enums"),
    HENLEGG_FEILREGISTRERT_BEHANDLING("familie.klage.henlegg-feilregistrert-behandling"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
