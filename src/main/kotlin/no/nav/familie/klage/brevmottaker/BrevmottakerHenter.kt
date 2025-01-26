package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerHenter(
    private val brevService: BrevService,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerHenter::class.java)

    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere {
        logger.debug("Henter brevmottakere for behandling {}.", behandlingId)
        val brev = brevService.hentBrev(behandlingId)
        return brev.mottakere ?: Brevmottakere()
    }
}
