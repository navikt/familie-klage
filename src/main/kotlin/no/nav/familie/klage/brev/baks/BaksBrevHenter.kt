package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.repository.findByIdOrThrow
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BaksBrevHenter(
    private val baksBrevRepository: BaksBrevRepository,
) {
    private val logger = LoggerFactory.getLogger(BaksBrevHenter::class.java)

    fun hentBrev(behandlingId: UUID): BaksBrev {
        logger.debug("Henter brev for behandling {}", behandlingId)
        return baksBrevRepository.findByIdOrThrow(behandlingId)
    }

    fun hentBrevEllerNull(behandlingId: UUID): BaksBrev? {
        logger.debug("Henter brev eller null for behandling {}", behandlingId)
        return baksBrevRepository.findByIdOrNull(behandlingId)
    }
}
