package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevRepository : RepositoryInterface<Brev, UUID>, InsertUpdateRepository<Brev>
