package no.nav.familie.klage.institusjon

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InstitusjonRepository :
    RepositoryInterface<Institusjon, UUID>,
    InsertUpdateRepository<Institusjon>
