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

    VELG_SIGNATUR_BASERT_PÅ_FAGSAK("familie-klage.velg-signatur-basert-paa-fagsak", "Permission"),

    // Release
    LEGG_TIL_BREVMOTTAKER_BAKS("familie-klage.legg-til-brevmottaker-baks", "Release"),
    SETT_BEHANDLINGSTEMA_OG_BEHANDLINGSTYPE_FOR_BAKS(
        "familie-klage.nav-24445-sett-behandlingstema-til-klage",
        "Release",
    ),
    BRUK_NYTT_BREV_BA_KS("familie-klage.bruk-nytt-brev-ba-ks", "Release"),
    KAN_MELLOMLAGRE_VURDERING("familie-klage.kan-mellomlagre-vurdering", "Release"),
    SKAL_BRUKE_KABAL_API_V4("familie-klage.skal-bruke-kabal-api-v4", "Release"),
    SKAL_BRUKE_NY_LØYPE_FOR_JOURNALFØRING("familie-klage.skal-bruke-ny-loype-for-journalforing", "Release"),
    SEND_BEHANDLING_ID_VED_OPPRETTING_AV_REVURDERING_KLAGE(
        "familie-klage.send-behandlingid-ved-oppretting-av-revurdering-klage",
        "Release",
    ),
    ;

    companion object {
        private val toggles: Map<String, Toggle> = values().associateBy { it.name }

        fun byToggleId(toggleId: String): Toggle = toggles[toggleId] ?: error("Finner ikke toggle for $toggleId")
    }
}
