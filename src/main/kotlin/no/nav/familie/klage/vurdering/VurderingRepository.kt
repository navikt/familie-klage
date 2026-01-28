package no.nav.familie.klage.vurdering

import no.nav.familie.klage.infrastruktur.repository.InsertUpdateRepository
import no.nav.familie.klage.infrastruktur.repository.RepositoryInterface
import no.nav.familie.klage.vurdering.domain.Vurdering
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VurderingRepository :
    RepositoryInterface<Vurdering, UUID>,
    InsertUpdateRepository<Vurdering>
