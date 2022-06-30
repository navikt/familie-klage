package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository

import java.util.UUID

@Repository
interface BehandlingsRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {


}