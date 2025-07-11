package no.nav.familie.klage.infrastruktur.featuretoggle

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
class FeatureToggleController(
    private val featureToggleService: FeatureToggleService,
) {
    private val featureTogglesIBruk: Set<Toggle> =
        setOf(
            Toggle.LEGG_TIL_BREVMOTTAKER_BAKS,
            Toggle.KAN_MELLOMLAGRE_VURDERING,
            Toggle.BRUK_NY_HENLEGG_BEHANDLING_MODAL,
        )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> = featureTogglesIBruk.associate { it.toggleId to featureToggleService.isEnabled(it) }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
    ): Boolean {
        val toggle = Toggle.byToggleId(toggleId)
        return featureToggleService.isEnabled(toggle)
    }
}
