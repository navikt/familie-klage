package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.dto.Avsnitt
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AvsnittRepository : RepositoryInterface<Avsnitt, UUID>, InsertUpdateRepository<Avsnitt> {

    @Query(
        """
            SELECT * from avsnitt
            WHERE behandling_id = :behandlingId
        """
    )
    fun hentAvsnittPåBehandlingId(
        behandlingId: UUID
    ): List<Avsnitt>?

    @Modifying
    @Query(
        """DELETE from avsnitt
            WHERE avsnitt.behandling_id = :behandlingId"""
    )
    fun slettAvsnittMedBehanldingId(behandlingId: UUID)
}
