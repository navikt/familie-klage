package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.BrevmottakerOppretterValidator.validerNyBrevmottakerPersonUtenIdent
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
        behandling.validerRedigerbarBehandlingOgBehandlingsstegBrev()

        val brev = brevService.hentBrev(behandlingId)
        val brevmottakere = brev.mottakere ?: Brevmottakere()
        val brevmottakerePersonerUtenIdent = brevmottakere.personer.filterIsInstance<BrevmottakerPersonUtenIdent>()
        validerNyBrevmottakerPersonUtenIdent(
            brukerensNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn,
            behandlingId = behandlingId,
            nyBrevmottakerPersonUtenIdent = nyBrevmottakerPersonUtenIdent,
            eksisterendeBrevmottakerePersonerUtenIdent = brevmottakerePersonerUtenIdent,
        )

        val aktivIdentForFagsak = fagsakService.hentFagsak(behandling.fagsakId).hentAktivIdent()

        val harBrevmottakerPersonBruker =
            brevmottakere.personer
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
                                    brevmottakere.personer.filter {
                                        when (it) {
                                            is BrevmottakerPersonMedIdent -> it.personIdent != aktivIdentForFagsak
                                            is BrevmottakerPersonUtenIdent -> true
                                        }
                                    }
                                filtrerteBrevmottakerPersoner + brevmottakerPersonUtenIdentSomSkalOpprettes
                            } else {
                                brevmottakere.personer + brevmottakerPersonUtenIdentSomSkalOpprettes
                            },
                        organisasjoner = brevmottakere.organisasjoner,
                    ),
            ),
        )

        return brevmottakerPersonUtenIdentSomSkalOpprettes
    }
}
