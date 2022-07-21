package no.nav.familie.klage.kabal

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KabalRepository: RepositoryInterface<OversendtKlageAnkeV3, UUID>,
    InsertUpdateRepository<OversendtKlageAnkeV3> {
}