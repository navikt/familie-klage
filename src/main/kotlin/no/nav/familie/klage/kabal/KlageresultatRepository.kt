package no.nav.familie.klage.kabal

import no.nav.familie.klage.kabal.domain.KlageinstansResultat
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KlageresultatRepository : RepositoryInterface<KlageinstansResultat, UUID>, InsertUpdateRepository<KlageinstansResultat> {

    fun findByBehandlingId(behandlingId: UUID): List<KlageinstansResultat>
}
