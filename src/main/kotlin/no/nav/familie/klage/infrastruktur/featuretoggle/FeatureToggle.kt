package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields

sealed class FeatureToggle(
    val toggleId: String,
    val beskrivelse: FeatureToggleBeskrivelse,
) {
    data object TestToggleMedStrategi : FeatureToggle(
        toggleId = "familie-klage.test-toggle-med-strategi",
        beskrivelse = FeatureToggleBeskrivelse.PERMISSION,
    )

    data object UtviklerMedVeilederrolle : FeatureToggle(
        toggleId = "familie.ef.sak.utviklere-med-veilederrolle",
        beskrivelse = FeatureToggleBeskrivelse.PERMISSION,
    )

    data object VelgSignaturBasertPåFagsak : FeatureToggle(
        toggleId = "familie-klage.velg-signatur-basert-paa-fagsak",
        beskrivelse = FeatureToggleBeskrivelse.PERMISSION,
    )

    data object SettPåVent : FeatureToggle(
        toggleId = "familie.klage.sett-pa-vent",
        beskrivelse = FeatureToggleBeskrivelse.RELEASE,
    )

    data object VisBrevmottakerBaks : FeatureToggle(
        toggleId = "familie-klage.vis-brevmottaker-baks",
        beskrivelse = FeatureToggleBeskrivelse.RELEASE,
    )

    data object LeggTilBrevmottakerBaks : FeatureToggle(
        toggleId = "familie-klage.legg-til-brevmottaker-baks",
        beskrivelse = FeatureToggleBeskrivelse.RELEASE,
    )

    data object SettBehandlingstemaOgBehandlingstypeForBaks : FeatureToggle(
        toggleId = "familie-klage.nav-24445-sett-behandlingstema-til-klage",
        beskrivelse = FeatureToggleBeskrivelse.RELEASE,
    )
}

enum class FeatureToggleBeskrivelse(val beskrivelse: String) {
    RELEASE("Release"), PERMISSION("Permission")
}

internal fun FeatureToggle.mapUnleashContextFields(): Map<String, String> {
    return when (this) {
        is FeatureToggle.TestToggleMedStrategi -> mapOf(
            UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
        )

        else -> emptyMap()
    }
}
