package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brevmottaker.BrevmottakerService
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HenleggBehandlingValidator(
    private val brevmottakerService: BrevmottakerService,
    private val featureToggleService: FeatureToggleService,
) {
    fun validerHenleggBehandlingDto(
        behandlingId: UUID,
        henleggBehandlingDto: HenleggBehandlingDto,
    ) {
        if (!featureToggleService.isEnabled(Toggle.BRUK_NY_HENLEGG_BEHANDLING_MODAL)) {
            feilHvis(henleggBehandlingDto.skalSendeHenleggelsesbrev && henleggBehandlingDto.årsak == HenlagtÅrsak.FEILREGISTRERT) {
                "Skal ikke sende brev hvis type er ulik trukket tilbake"
            }
            return
        }

        henleggBehandlingDto.valider()

        val brevmottakerBrukerFraDto = henleggBehandlingDto.finnNyBrevmottakerBruker()
        val brevmottakerBrukerFraBehandling = brevmottakerService.utledBrevmottakerBrukerFraBehandling(behandlingId)
        val erBrevmottakerBrukerLik = brevmottakerBrukerFraDto != null && brevmottakerBrukerFraDto.erLik(brevmottakerBrukerFraBehandling)

        if (brevmottakerBrukerFraDto != null && !erBrevmottakerBrukerLik) {
            throw ApiFeil.badRequest("Bruker fra dto er ulik bruker utledet fra behandlingen.")
        }
    }
}
