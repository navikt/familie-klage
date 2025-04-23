package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottaker
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonUtenIdent
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

private val MOTTAKER_ROLLER_HVOR_BRUKER_SKAL_SLETTES_VED_OPPRETTELSE =
    setOf(
        MottakerRolle.DØDSBO,
        MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
    )

@Component
class BrevmottakerOppretter(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val brevService: BrevService,
    private val brevRepository: BrevRepository,
    private val personopplysningerService: PersonopplysningerService,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerOppretter::class.java)

    @Transactional
    fun opprettBrevmottaker(
        behandlingId: UUID,
        nyBrevmottaker: NyBrevmottaker,
    ): Brevmottaker =
        when (nyBrevmottaker) {
            is NyBrevmottakerOrganisasjon -> throw UnsupportedOperationException("${nyBrevmottaker::class.simpleName} er ikke støttet.")
            is NyBrevmottakerPersonMedIdent -> throw UnsupportedOperationException("${nyBrevmottaker::class.simpleName} er ikke støttet.")
            is NyBrevmottakerPersonUtenIdent -> opprettBrevmottakerPersonUtenIdent(behandlingId, nyBrevmottaker)
        }

    private fun opprettBrevmottakerPersonUtenIdent(
        behandlingId: UUID,
        nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent,
    ): Brevmottaker {
        logger.debug("Oppretter brevmottaker for behandling {}.", behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)
        validerKorrektBehandlingssteg(behandling)

        val brev = brevService.hentBrev(behandlingId)
        val brevmottakerPersoner = brev.mottakere?.personer ?: emptyList()
        val brevmottakerePersonerUtenIdent = brevmottakerPersoner.filterIsInstance<BrevmottakerPersonUtenIdent>()
        validerNyBrevmottakerPersonUtenIdent(
            behandlingId,
            nyBrevmottakerPersonUtenIdent,
            brevmottakerePersonerUtenIdent,
        )

        val aktivIdentForFagsak = fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()

        val harBrevmottakerPersonBruker =
            brevmottakerPersoner
                .filterIsInstance<BrevmottakerPersonMedIdent>()
                .filter { it.mottakerRolle == MottakerRolle.BRUKER }
                .any { it.personIdent == aktivIdentForFagsak }

        val skalSletteBrevmottakerPersonBruker =
            harBrevmottakerPersonBruker &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle in MOTTAKER_ROLLER_HVOR_BRUKER_SKAL_SLETTES_VED_OPPRETTELSE

        val brevmottakerPersonUtenIdentSomSkalOpprettes =
            BrevmottakerPersonUtenIdent.opprettFra(
                id = UUID.randomUUID(),
                nyBrevmottakerPersonUtenIdent = nyBrevmottakerPersonUtenIdent,
            )

        brevRepository.update(
            brev.copy(
                mottakere =
                    Brevmottakere(
                        personer =
                            if (skalSletteBrevmottakerPersonBruker) {
                                val filtrerteBrevmottakerPersoner =
                                    brevmottakerPersoner.filter {
                                        when (it) {
                                            is BrevmottakerPersonMedIdent -> it.personIdent != aktivIdentForFagsak
                                            is BrevmottakerPersonUtenIdent -> true
                                        }
                                    }
                                filtrerteBrevmottakerPersoner + brevmottakerPersonUtenIdentSomSkalOpprettes
                            } else {
                                brevmottakerPersoner + brevmottakerPersonUtenIdentSomSkalOpprettes
                            },
                    ),
            ),
        )

        return brevmottakerPersonUtenIdentSomSkalOpprettes
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

    private fun validerNyBrevmottakerPersonUtenIdent(
        behandlingId: UUID,
        nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent,
        eksisterendeBrevmottakerePersonerUtenIdent: List<BrevmottakerPersonUtenIdent>,
    ) {
        val brukerensNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        val eksisterendeMottakerRoller = eksisterendeBrevmottakerePersonerUtenIdent.map { it.mottakerRolle }
        when {
            eksisterendeMottakerRoller.any { it == nyBrevmottakerPersonUtenIdent.mottakerRolle } -> {
                throw Feil(
                    "Kan ikke ha duplikate MottakerRolle. ${nyBrevmottakerPersonUtenIdent.mottakerRolle} finnes allerede for $behandlingId.",
                )
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE &&
                nyBrevmottakerPersonUtenIdent.navn != brukerensNavn -> {
                throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn for $behandlingId.")
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == MottakerRolle.DØDSBO &&
                !nyBrevmottakerPersonUtenIdent.navn.contains(brukerensNavn) -> {
                throw Feil("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn for $behandlingId.")
            }

            nyBrevmottakerPersonUtenIdent.mottakerRolle == MottakerRolle.DØDSBO &&
                eksisterendeBrevmottakerePersonerUtenIdent.isNotEmpty() -> {
                throw Feil("Kan ikke legge til dødsbo når det allerede finnes brevmottakere for $behandlingId.")
            }

            eksisterendeMottakerRoller.any { it == MottakerRolle.DØDSBO } -> {
                throw Feil("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo for $behandlingId.")
            }

            MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE in eksisterendeMottakerRoller &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== MottakerRolle.VERGE &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== MottakerRolle.FULLMAKT -> {
                throw Feil("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig for $behandlingId.")
            }

            eksisterendeMottakerRoller.isNotEmpty() &&
                MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE !in eksisterendeMottakerRoller &&
                nyBrevmottakerPersonUtenIdent.mottakerRolle !== MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE -> {
                throw Feil("Kan kun legge til bruker med utenlandsk adresse om det finnes en brevmottaker allerede for $behandlingId.")
            }
        }
    }
}
