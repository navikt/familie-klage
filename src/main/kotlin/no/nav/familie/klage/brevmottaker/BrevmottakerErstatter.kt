package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brevmottaker.BrevmottakerUtil.validerBrevmottakere
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.infrastruktur.repository.findByIdOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BrevmottakerErstatter(
    private val behandlingService: BehandlingService,
    private val brevRepository: BrevRepository,
) {
    private val logger = LoggerFactory.getLogger(BrevmottakerErstatter::class.java)

    @Transactional
    fun erstattBrevmottakere(
        behandlingId: UUID,
        brevmottakere: Brevmottakere,
    ): Brevmottakere {
        logger.debug("Erstatter brevmottakere for behandling {}.", behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        behandling.validerRedigerbarBehandlingOgBehandlingsstegBrev()
        validerBrevmottakere(behandlingId, brevmottakere)
        val eksisterendeBrev = brevRepository.findByIdOrThrow(behandlingId)
        val oppdatertBrev = brevRepository.update(eksisterendeBrev.copy(mottakere = brevmottakere))
        return oppdatertBrev.mottakere ?: error("Fant ikke brevmottakere for behandling $behandlingId.")
    }
}
