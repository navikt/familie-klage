package no.nav.familie.klage.brev.baks.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerOppretter(
    private val behandlingService: BehandlingService,
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerOppretter::class.java)

    @Transactional
    fun opprettBrevmottaker(behandlingId: UUID, nyBrevmottaker: NyBrevmottaker): Brevmottaker {
        logger.debug("Oppretter brevmottaker for behandling {}", behandlingId)
        validerRedigerbarBehandling(behandlingId)
        validerNyBrevmottaker(behandlingId, nyBrevmottaker)
        val brevmottaker = Brevmottaker.opprett(behandlingId, nyBrevmottaker)
        return brevmottakerRepository.insert(brevmottaker)
    }

    private fun validerRedigerbarBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Behandling $behandlingId er låst for videre behandling.")
        }
    }

    private fun validerNyBrevmottaker(behandlingId: UUID, nyBrevmottaker: NyBrevmottaker) {
        val eksisterendeBrevmottakere = brevmottakerRepository.findByBehandlingId(behandlingId)
        val brukerensNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        val eksisterendeMottakertyper = eksisterendeBrevmottakere.map { it.mottakertype }
        when {
            eksisterendeMottakertyper.any { it == nyBrevmottaker.mottakertype } -> {
                throw Feil("Kan ikke ha duplikate mottakertyper. ${nyBrevmottaker.mottakertype} finnes allerede.")
            }

            nyBrevmottaker.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE && nyBrevmottaker.navn != brukerensNavn -> {
                throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
            }

            nyBrevmottaker.mottakertype == Mottakertype.DØDSBO && !nyBrevmottaker.navn.contains(brukerensNavn) -> {
                throw Feil("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn.")
            }

            nyBrevmottaker.mottakertype == Mottakertype.DØDSBO && eksisterendeBrevmottakere.isNotEmpty() -> {
                throw Feil("Kan ikke legge til dødsbo når det allerede finnes brevmottakere.")
            }

            eksisterendeMottakertyper.any { it == Mottakertype.DØDSBO } -> {
                throw Feil("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo.")
            }

            Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE in eksisterendeMottakertyper &&
                nyBrevmottaker.mottakertype !== Mottakertype.VERGE &&
                nyBrevmottaker.mottakertype !== Mottakertype.FULLMEKTIG
            -> {
                throw Feil("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig.")
            }

            eksisterendeMottakertyper.isNotEmpty() &&
                Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE !in eksisterendeMottakertyper &&
                nyBrevmottaker.mottakertype !== Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
            -> {
                throw Feil("Kan kun legge til bruker med utenlandsk adresse om det finnes en brevmottaker allerede.")
            }
        }
    }
}
