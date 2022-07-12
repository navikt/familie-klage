package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.dto.Avsnitt
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AvsnittRepository: RepositoryInterface<Avsnitt, UUID>, InsertUpdateRepository<Avsnitt>