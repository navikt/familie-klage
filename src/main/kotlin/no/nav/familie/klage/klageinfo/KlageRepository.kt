package no.nav.familie.klage.klageinfo

import no.nav.familie.klage.klageinfo.domain.Klage
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KlageRepository : RepositoryInterface<Klage, UUID>, InsertUpdateRepository<Klage> {

    fun findByBehandlingId(behandlingId: UUID): Klage
}
