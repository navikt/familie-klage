package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.BrevmottakerUtil.validerBrevmottakere
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
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle.MANUELL_BREVMOTTAKER_ORGANISASJON
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

private val MOTTAKER_ROLLER_HVOR_BRUKER_SKAL_LEGGES_TIL_VED_SLETTING =
    setOf(
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
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerSletter::class.java)

    @Transactional
    fun slettBrevmottaker(
        behandlingId: UUID,
        slettbarBrevmottaker: SlettbarBrevmottaker,
    ) = when (slettbarBrevmottaker) {
        is SlettbarBrevmottakerOrganisasjon -> slettBrevmottakerOrganisasjon(behandlingId, slettbarBrevmottaker)
        is SlettbarBrevmottakerPersonMedIdent -> throw UnsupportedOperationException("Sletting av person med ident er ikke støttet.")
        is SlettbarBrevmottakerPersonUtenIdent -> slettBrevmottakerPersonUtenIdent(behandlingId, slettbarBrevmottaker)
    }

    private fun slettBrevmottakerPersonUtenIdent(
        behandlingId: UUID,
        slettBrevmottakerPersonUtenIdent: SlettbarBrevmottakerPersonUtenIdent,
    ) {
        logger.debug("Sletter brevmottaker {} for behandling {}.", slettBrevmottakerPersonUtenIdent.id, behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        behandling.validerRedigerbarBehandlingOgBehandlingsstegBrev()

        val brev = brevService.hentBrev(behandlingId)
        val brevmottakere = brev.mottakere ?: Brevmottakere()

        val brevmottakerPersonSomSkalSlettes =
            brevmottakere.personer
                .filterIsInstance<BrevmottakerPersonUtenIdent>()
                .find { it.id == slettBrevmottakerPersonUtenIdent.id }

        if (brevmottakerPersonSomSkalSlettes == null) {
            throw Feil("Brevmottaker ${slettBrevmottakerPersonUtenIdent.id} kan ikke slettes da den ikke finnes.")
        }

        val nyeBrevmottakerPersoner =
            brevmottakere.personer.filter {
                when (it) {
                    is BrevmottakerPersonMedIdent -> true
                    is BrevmottakerPersonUtenIdent -> it.id != slettBrevmottakerPersonUtenIdent.id
                }
            }

        val fagsakAktivIdent = fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()

        val harBrevmottakerPersonBruker =
            brevmottakere.personer
                .filterIsInstance<BrevmottakerPersonMedIdent>()
                .filter { it.mottakerRolle == MottakerRolle.BRUKER }
                .any { it.personIdent == fagsakAktivIdent }

        val skalLeggeTilBrevmottakerPersonBrukerVedSletting =
            !harBrevmottakerPersonBruker &&
                MOTTAKER_ROLLER_HVOR_BRUKER_SKAL_LEGGES_TIL_VED_SLETTING.contains(brevmottakerPersonSomSkalSlettes.mottakerRolle)

        val nyeBrevmottakere =
            Brevmottakere(
                personer =
                    if (skalLeggeTilBrevmottakerPersonBrukerVedSletting) {
                        val brevmottakerPersonBruker =
                            BrevmottakerPersonMedIdent(
                                personIdent = fagsakAktivIdent,
                                navn = personopplysningerService.hentPersonopplysninger(behandlingId).navn,
                                mottakerRolle = MottakerRolle.BRUKER,
                            )
                        nyeBrevmottakerPersoner + brevmottakerPersonBruker
                    } else {
                        nyeBrevmottakerPersoner
                    },
                organisasjoner = brevmottakere.organisasjoner,
            )

        validerBrevmottakere(behandlingId, nyeBrevmottakere)

        brevRepository.update(
            brev.copy(
                mottakere = nyeBrevmottakere,
            ),
        )
    }

    private fun slettBrevmottakerOrganisasjon(
        behandlingId: UUID,
        slettbarBrevmottaker: SlettbarBrevmottakerOrganisasjon,
    ) {
        if (!featureToggleService.isEnabled(MANUELL_BREVMOTTAKER_ORGANISASJON)) {
            throw Feil("Feature toggle for å slette manuell brevmottaker organisasjon er ikke aktivert.")
        }

        logger.debug("Sletter brevmottaker {} for behandling {}.", slettbarBrevmottaker.organisasjonsnummer, behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        behandling.validerRedigerbarBehandlingOgBehandlingsstegBrev()

        val brev = brevService.hentBrev(behandlingId)
        val brevmottakere = brev.mottakere ?: Brevmottakere()

        val brevmottakerOrganisasjonSomSkalSlettes =
            brevmottakere.organisasjoner
                .find { it.organisasjonsnummer == slettbarBrevmottaker.organisasjonsnummer }

        if (brevmottakerOrganisasjonSomSkalSlettes == null) {
            throw Feil("Brevmottaker ${slettbarBrevmottaker.organisasjonsnummer} kan ikke slettes da den ikke finnes.")
        }

        val nyeBrevmottakerOrganisasjoner =
            brevmottakere.organisasjoner.filter {
                it.organisasjonsnummer != slettbarBrevmottaker.organisasjonsnummer
            }

        val nyeBrevmottakere =
            brevmottakere.copy(
                organisasjoner = nyeBrevmottakerOrganisasjoner,
            )

        validerBrevmottakere(behandlingId, nyeBrevmottakere)

        brevRepository.update(
            brev.copy(
                mottakere = nyeBrevmottakere,
            ),
        )
    }
}
