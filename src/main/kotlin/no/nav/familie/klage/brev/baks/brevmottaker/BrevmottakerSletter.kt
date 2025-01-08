package no.nav.familie.klage.brev.baks.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.infrastruktur.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerSletter(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerSletter::class.java)

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID) {
        logger.debug("Sletter brevmottaker for behandling {}", behandlingId)
        validerRedigerbarBehandling(behandlingId)
        validerBrevmottakerEksiterer(brevmottakerId)
        brevmottakerRepository.deleteById(brevmottakerId)
    }

    private fun validerRedigerbarBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status.erLåstForVidereBehandling()) {
            throw Feil("Behandling $behandlingId er låst for videre behandling.")
        }
    }

    private fun validerBrevmottakerEksiterer(brevmottakerId: UUID) {
        val eksisterer = brevmottakerRepository.existsById(brevmottakerId)
        if (!eksisterer) {
            throw Feil("Brevmottaker med id $brevmottakerId finnes ikke.")
        }
    }
}
