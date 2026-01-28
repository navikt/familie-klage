package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brevmottaker.BrevmottakerService
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.FULLMAKT
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.INSTITUSJON
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerDto
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HenleggBehandlingValidator(
    private val brevmottakerService: BrevmottakerService,
    private val fagsakService: FagsakService,
) {
    fun validerHenleggBehandlingDto(
        behandlingId: UUID,
        henleggBehandlingDto: HenleggBehandlingDto,
    ) {
        henleggBehandlingDto.valider()

        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        if (fagsak.erInstitusjonssak()) {
            validerAtBareInstitusjonOgFullmaktErBrevmottaker(henleggBehandlingDto.nyeBrevmottakere)
        }

        val brevmottakerBrukerFraDto = henleggBehandlingDto.finnNyBrevmottakerBruker()
        val brevmottakerBrukerFraBehandling = brevmottakerService.utledBrevmottakerBrukerFraBehandling(behandlingId)
        val erBrevmottakerBrukerLik = brevmottakerBrukerFraDto != null && brevmottakerBrukerFraDto.erLik(brevmottakerBrukerFraBehandling)

        if (brevmottakerBrukerFraDto != null && !erBrevmottakerBrukerLik) {
            throw ApiFeil.badRequest("Innsendt bruker samsvarer ikke med bruker utledet fra behandlingen.")
        }
    }

    private fun validerAtBareInstitusjonOgFullmaktErBrevmottaker(mottakere: List<NyBrevmottakerDto>) {
        val harUgyldigBrevmottaker = mottakere.any { it.mottakerRolle !in setOf(INSTITUSJON, FULLMAKT) }
        brukerfeilHvis(harUgyldigBrevmottaker) {
            "I institusjonssaker kan brevmottakere kun ha rollene $INSTITUSJON og $FULLMAKT"
        }
    }
}
