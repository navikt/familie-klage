package no.nav.familie.klage.infrastruktur.featuretoggle

import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    val unleashNextService: UnleashNextService,
) {
    fun isEnabled(toggle: Toggle): Boolean {
        return unleashNextService.isEnabled(toggle)
    }
}

enum class Toggle(val toggleId: String, val beskrivelse: String? = null) {
    PLACEHOLDER("ktlint-liker-ikke-tomme-enums"),

    // Permission
    UTVIKLER_MED_VEILEDERRROLLE("familie.ef.sak.utviklere-med-veilederrolle", "Permission"),

    VELG_SIGNATUR_BASERT_PÅ_FAGSAK("familie-klage.velg-signatur-basert-paa-fagsak", "Permission"),

    // Release
    SETT_PÅ_VENT("familie.klage.sett-pa-vent", "Release"),
    VIS_BREVMOTTAKER_BAKS("familie-klage.vis-brevmottaker-baks", "Release"),
    LEGG_TIL_BREVMOTTAKER_BAKS("familie-klage.legg-til-brevmottaker-baks", "Release"),
    SETT_BEHANDLINGSTEMA_OG_BEHANDLINGSTYPE_FOR_BAKS(
        "familie-klage.nav-24445-sett-behandlingstema-til-klage",
        "Release",
    ),
    TEST_TOGGLE_MED_STRATEGI("familie-klage.test-toggle-med-strategi"),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle {
            return toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
        }
    }
}
