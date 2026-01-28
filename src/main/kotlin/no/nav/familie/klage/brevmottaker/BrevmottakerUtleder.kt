package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPerson
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.institusjon.Institusjon
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerUtleder(
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
) {
    fun utledInitielleBrevmottakere(behandlingId: UUID): Brevmottakere {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val institusjon = fagsak.institusjon
        val (personer, organisasjoner) =
            if (institusjon == null) {
                listOf(utledBrevmottakerBrukerFraBehandling(behandlingId)) to emptyList()
            } else {
                emptyList<BrevmottakerPerson>() to listOf(institusjon.tilBrevmottakerOrganisasjon())
            }
        return Brevmottakere(
            personer = personer,
            organisasjoner = organisasjoner,
        )
    }

    fun utledBrevmottakerBrukerFraBehandling(behandlingId: UUID): BrevmottakerPersonMedIdent {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        return BrevmottakerPersonMedIdent(
            personIdent = fagsak.hentAktivIdent(),
            navn = personopplysninger.navn,
            mottakerRolle = MottakerRolle.BRUKER,
        )
    }

    private fun Institusjon.tilBrevmottakerOrganisasjon(): BrevmottakerOrganisasjon =
        BrevmottakerOrganisasjon(
            organisasjonsnummer = orgNummer,
            organisasjonsnavn = navn,
            mottakerRolle = MottakerRolle.INSTITUSJON,
        )
}
