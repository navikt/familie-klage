package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(val unleashService: UnleashService) {

    fun isEnabled(toggle: Toggle): Boolean {
        return unleashService.isEnabled(toggle.toggleId, false)
    }

    fun isEnabled(toggle: Toggle, defaultVerdi: Boolean): Boolean {
        return unleashService.isEnabled(toggle.toggleId, defaultVerdi)
    }
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    PLACEHOLDER("ktlint-liker-ikke-tomme-enums"),

    // Permission
    UTVIKLER_MED_VEILEDERRROLLE("familie.ef.sak.utviklere-med-veilederrolle", "Permission"),

    VELG_SIGNATUR_BASERT_PÅ_FAGSAK("familie-klage.velg-signatur-basert-paa-fagsak", "Permission"),

    // Release
    SETT_PÅ_VENT("familie.klage.sett-pa-vent", "Release"),
    SKAL_BRUKE_KABAL_API_V4("familie-klage.skal-bruke-kabal-api-v4", "Release"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
