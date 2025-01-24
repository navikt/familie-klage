package no.nav.familie.klage.brev.brevmottaker.ef

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.BrevmottakerUtil.validerMinimumEnMottaker
import no.nav.familie.klage.brev.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.brev.dto.BrevmottakereDto
import no.nav.familie.klage.brev.dto.tilDomene
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EfBrevmottakerService(
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val brevRepository: BrevRepository,
) {
    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere {
        val brev = brevRepository.findByIdOrThrow(behandlingId)
        return brev.mottakere ?: Brevmottakere()
    }

    fun settBrevmottakere(behandlingId: UUID, brevmottakere: BrevmottakereDto) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        brevService.validerKanLageBrev(behandling)

        val mottakere = brevmottakere.tilDomene()

        validerUnikeBrevmottakere(mottakere)
        validerMinimumEnMottaker(mottakere)

        val brev = brevRepository.findByIdOrThrow(behandlingId)
        brevRepository.update(brev.copy(mottakere = mottakere))
    }
}
