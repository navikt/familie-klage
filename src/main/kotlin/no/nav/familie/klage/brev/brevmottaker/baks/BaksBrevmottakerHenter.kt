package no.nav.familie.klage.brev.brevmottaker.baks

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brevmottakere
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BaksBrevmottakerHenter(
    private val brevService: BrevService,
) {
    private val logger = LoggerFactory.getLogger(BaksBrevmottakerHenter::class.java)

    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere {
        logger.debug("Henter brevmottakere for behandling {}", behandlingId)
        val brev = brevService.hentBrev(behandlingId)
        if (brev.mottakere == null) {
            throw IllegalStateException("Fant ikke mottakere i brev for behandling ${brev.behandlingId}")
        }
        return brev.mottakere
    }
}
