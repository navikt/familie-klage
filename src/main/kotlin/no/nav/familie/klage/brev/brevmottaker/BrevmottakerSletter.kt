package no.nav.familie.klage.brev.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.brev.domain.MottakerRolle
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
    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID) {
        logger.debug("Sletter brevmottaker {} for behandling {}.", brevmottakerId, behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)

        val brev = brevService.hentBrev(behandlingId)
        val brevmottakerPersoner = (brev.mottakere?.personer ?: emptyList())

        val brevmottakerPersonSomSkalSlettes = brevmottakerPersoner
            .filterIsInstance<BrevmottakerPersonUtenIdent>()
            .find { it.id == brevmottakerId }

        if (brevmottakerPersonSomSkalSlettes == null) {
            throw Feil("Brevmottaker $brevmottakerId kan ikke slettes da den ikke finnes.")
        }

        val nyeBrevmottakerPersoner = brevmottakerPersoner.filter {
            when (it) {
                is BrevmottakerPersonMedIdent -> true
                is BrevmottakerPersonUtenIdent -> it.id != brevmottakerId
            }
        }

        val fagsakAktivIdent = fagsakService.hentFagsakForBehandling(behandlingId).hentAktivIdent()

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
