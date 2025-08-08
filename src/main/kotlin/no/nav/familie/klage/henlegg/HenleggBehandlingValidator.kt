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
) {
    fun validerHenleggBehandlingDto(
        behandlingId: UUID,
        henleggBehandlingDto: HenleggBehandlingDto,
    ) {
        henleggBehandlingDto.valider()

        val brevmottakerBrukerFraDto = henleggBehandlingDto.finnNyBrevmottakerBruker()
        val brevmottakerBrukerFraBehandling = brevmottakerService.utledBrevmottakerBrukerFraBehandling(behandlingId)
        val erBrevmottakerBrukerLik = brevmottakerBrukerFraDto != null && brevmottakerBrukerFraDto.erLik(brevmottakerBrukerFraBehandling)

        if (brevmottakerBrukerFraDto != null && !erBrevmottakerBrukerLik) {
            throw ApiFeil.badRequest("Innsendt bruker samsvarer ikke med bruker utledet fra behandlingen.")
        }
    }
}
