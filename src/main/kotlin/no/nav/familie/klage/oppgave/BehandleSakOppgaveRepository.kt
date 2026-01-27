package no.nav.familie.klage.oppgave

import no.nav.familie.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.familie.klage.infrastruktur.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandleSakOppgaveRepository :
    RepositoryInterface<BehandleSakOppgave, UUID>,
    InsertUpdateRepository<BehandleSakOppgave> {
    fun findByBehandlingId(behandlingId: UUID): BehandleSakOppgave?
}
