package no.nav.familie.klage.vurdering

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VurderingRepository : RepositoryInterface<Vurdering, UUID>, InsertUpdateRepository<Vurdering> {
    fun findByBehandlingId(behandlingId: UUID): Vurdering

    @Query(
        """SELECT DISTINCT vurdering.vedtak FROM vurdering 
              WHERE vurdering.behandling_id = :behandling_id AND vurdering.vedtak IS NOT NULL"""
    )
    fun findVedtakByBehandlingIdOrThrow(behandling_id: UUID): Vedtak?
}
