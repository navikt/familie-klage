package no.nav.familie.klage.infrastruktur.featuretoggle

import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    val unleashNextService: UnleashNextService,
) {
    fun isEnabled(toggle: Toggle): Boolean = unleashNextService.isEnabled(toggle)
}

enum class Toggle(
    val toggleId: String,
    val beskrivelse: String? = null,
) {
    PLACEHOLDER("ktlint-liker-ikke-tomme-enums"),

    // Permission
    UTVIKLER_MED_VEILEDERRROLLE("familie.ef.sak.utviklere-med-veilederrolle", "Permission"),

    // Release
    ;

    companion object {
        private val toggles: Map<String, Toggle> = entries.associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle = toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
    }
}
