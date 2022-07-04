package no.nav.familie.klage.vurdering

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Repository

@Repository
interface VurderingRepository : RepositoryInterface<Vurdering, Vurdering>, InsertUpdateRepository<Vurdering> {


}