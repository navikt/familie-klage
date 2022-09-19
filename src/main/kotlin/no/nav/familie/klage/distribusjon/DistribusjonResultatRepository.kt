package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DistribusjonResultatRepository : RepositoryInterface<DistribusjonResultat, UUID>, InsertUpdateRepository<DistribusjonResultat>
