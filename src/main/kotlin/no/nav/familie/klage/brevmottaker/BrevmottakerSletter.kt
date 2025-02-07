package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottaker
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonUtenIdent
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

private val MOTTAKER_ROLLER_HVOR_BRUKER_SKAL_LEGGES_TIL_VED_SLETTING = setOf(
    MottakerRolle.DØDSBO,
    MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
)

@Component
class BrevmottakerSletter(
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val brevRepository: BrevRepository,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerSletter::class.java)

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, slettbarBrevmottaker: SlettbarBrevmottaker) {
        return when (slettbarBrevmottaker) {
            is SlettbarBrevmottakerOrganisasjon -> throw UnsupportedOperationException("Sletting av organisasjon er ikke støttet.")
            is SlettbarBrevmottakerPersonMedIdent -> throw UnsupportedOperationException("Sletting av person med ident er ikke støttet.")
            is SlettbarBrevmottakerPersonUtenIdent -> slettBrevmottakerPersonUtenIdent(behandlingId, slettbarBrevmottaker)
        }
    }

    private fun slettBrevmottakerPersonUtenIdent(
        behandlingId: UUID,
        slettBrevmottakerPersonUtenIdent: SlettbarBrevmottakerPersonUtenIdent,
    ) {
        logger.debug("Sletter brevmottaker {} for behandling {}.", slettBrevmottakerPersonUtenIdent.id, behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)

        val brev = brevService.hentBrev(behandlingId)
        val brevmottakerPersoner = (brev.mottakere?.personer ?: emptyList())

        val brevmottakerPersonSomSkalSlettes = brevmottakerPersoner
            .filterIsInstance<BrevmottakerPersonUtenIdent>()
            .find { it.id == slettBrevmottakerPersonUtenIdent.id }

        if (brevmottakerPersonSomSkalSlettes == null) {
            throw Feil("Brevmottaker ${slettBrevmottakerPersonUtenIdent.id} kan ikke slettes da den ikke finnes.")
        }

        val nyeBrevmottakerPersoner = brevmottakerPersoner.filter {
            when (it) {
                is BrevmottakerPersonMedIdent -> true
                is BrevmottakerPersonUtenIdent -> it.id != slettBrevmottakerPersonUtenIdent.id
            }
        }

        val fagsakAktivIdent = fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()

        val harBrevmottakerPersonBruker = brevmottakerPersoner
            .filterIsInstance<BrevmottakerPersonMedIdent>()
            .filter { it.mottakerRolle == MottakerRolle.BRUKER }
            .any { it.personIdent == fagsakAktivIdent }

        val skalLeggeTilBrevmottakerPersonBrukerVedSletting = !harBrevmottakerPersonBruker &&
            MOTTAKER_ROLLER_HVOR_BRUKER_SKAL_LEGGES_TIL_VED_SLETTING.contains(brevmottakerPersonSomSkalSlettes.mottakerRolle)

        val nyeBrevmottakere = Brevmottakere(
            personer = if (skalLeggeTilBrevmottakerPersonBrukerVedSletting) {
                val brevmottakerPersonBruker = BrevmottakerPersonMedIdent(
                    personIdent = fagsakAktivIdent,
                    navn = personopplysningerService.hentPersonopplysninger(behandlingId).navn,
                    mottakerRolle = MottakerRolle.BRUKER,
                )
                nyeBrevmottakerPersoner + brevmottakerPersonBruker
            } else {
                nyeBrevmottakerPersoner
            },
        )

        brevRepository.update(
            brev.copy(
                mottakere = nyeBrevmottakere,
            ),
        )
    }

    private fun validerRedigerbarBehandling(behandling: Behandling) {
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Behandling ${behandling.id} er låst for videre behandling.")
        }
    }

    private fun validerKorrektBehandlingssteg(behandling: Behandling) {
        if (behandling.steg != StegType.BREV) {
            throw Feil("Behandlingen er i steg ${behandling.steg}, forventet steg ${StegType.BREV}.")
        }
    }
}
