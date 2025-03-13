package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
class FeatureToggleController(
    private val featureToggleService: FeatureToggleService,
) {
    private val aktiveFeatureToggles: Set<FeatureToggle> = setOf(
        FeatureToggle.SettPåVent,
        FeatureToggle.VisBrevmottakerBaks,
        FeatureToggle.LeggTilBrevmottakerBaks,
        FeatureToggle.TestToggleMedStrategi,
    )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> {
        return aktiveFeatureToggles.associate { it.toggleId to featureToggleService.isEnabledMedContextField(it) }
    }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
        @RequestParam("defaultverdi") defaultVerdi: Boolean? = false,
    ): Boolean {
        val toggle = Toggle.byToggleId(toggleId)
        return featureToggleService.isEnabled(toggle, defaultVerdi ?: false)
    }
}
