package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevmottakerMedAdresseRepository :
    RepositoryInterface<BrevmottakerMedAdresse, UUID>,
    InsertUpdateRepository<BrevmottakerMedAdresse> {
    fun findByBehandlingId(behandlingId: UUID): List<BrevmottakerMedAdresse>

    fun deleteByBehandlingId(behandlingId: UUID)
}
