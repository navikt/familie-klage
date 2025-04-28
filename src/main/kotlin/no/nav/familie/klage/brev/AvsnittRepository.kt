package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Avsnitt
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AvsnittRepository :
    RepositoryInterface<Avsnitt, UUID>,
    InsertUpdateRepository<Avsnitt> {
    fun findByBehandlingId(behandlingId: UUID): List<Avsnitt>

    @Modifying
    @Query("""DELETE FROM avsnitt WHERE avsnitt.behandling_id = :behandlingId""")
    fun slettAvsnittMedBehandlingId(behandlingId: UUID)
}
