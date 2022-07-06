package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PersonopplysningerRepository : RepositoryInterface<Personopplysninger, UUID>, InsertUpdateRepository<Personopplysninger> {
    fun findByPersonIdent(personIdent: UUID): Personopplysninger
}