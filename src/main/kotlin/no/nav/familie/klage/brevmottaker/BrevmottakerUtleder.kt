package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerUtleder(
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
) {
    fun utledInitielleBrevmottakere(behandlingId: UUID): Brevmottakere =
        Brevmottakere(
            personer = listOf(utledBrevmottakerBrukerFraBehandling(behandlingId)),
            organisasjoner = emptyList(),
        )

    fun utledBrevmottakerBrukerFraBehandling(behandlingId: UUID): BrevmottakerPersonMedIdent {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        return BrevmottakerPersonMedIdent(
            personIdent = fagsak.hentAktivIdent(),
            navn = personopplysninger.navn,
            mottakerRolle = MottakerRolle.BRUKER,
        )
    }
}
