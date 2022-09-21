package no.nav.familie.klage.kabal

import no.nav.familie.klage.kabal.domain.Klageresultat
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KlageresultatRepository : RepositoryInterface<Klageresultat, UUID>, InsertUpdateRepository<Klageresultat> {

    fun findByBehandlingId(behandlingId: UUID): List<Klageresultat>
}
