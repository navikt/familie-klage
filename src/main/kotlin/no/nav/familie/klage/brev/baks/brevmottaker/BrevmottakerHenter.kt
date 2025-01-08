package no.nav.familie.klage.brev.baks.brevmottaker

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerHenter(
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerHenter::class.java)

    fun hentBrevmottakere(behandlingId: UUID): List<Brevmottaker> {
        logger.debug("Henter brevmottaker for behandling {}", behandlingId)
        Thread.sleep(500)
        return brevmottakerRepository.findByBehandlingId(behandlingId)
    }
}
