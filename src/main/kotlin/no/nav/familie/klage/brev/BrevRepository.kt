package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.dto.Avsnitt
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevRepository: RepositoryInterface<Brev, UUID>, InsertUpdateRepository<Brev> {

    @Query(
        """SELECT DISTINCT behandling.person_id FROM behandling 
              WHERE behandling.id = :behandling_id"""
    )
    fun findPersonIdByBehandlingId(behandling_id: UUID): String

    @Query(
        """
            SELECT * from avsnitt
            WHERE behandling_id = :behandlingId
        """
    )
    fun hentAvsnittPåBehandlingId(
        behandlingId: UUID
    ): List<Avsnitt>?
}