package no.nav.familie.klage.behandlingshistorikk

import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>, InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>

}