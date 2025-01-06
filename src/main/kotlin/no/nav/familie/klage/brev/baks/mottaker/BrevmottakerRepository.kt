package no.nav.familie.klage.brev.baks.mottaker

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerRepository :
    RepositoryInterface<Brevmottaker, UUID>,
    InsertUpdateRepository<Brevmottaker> {
    fun findByBehandlingId(behandlingId: UUID): List<Brevmottaker>
}
