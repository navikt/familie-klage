package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.brev.baks.domain.BaksBrev
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BaksBrevRepository : RepositoryInterface<BaksBrev, UUID>, InsertUpdateRepository<BaksBrev>
