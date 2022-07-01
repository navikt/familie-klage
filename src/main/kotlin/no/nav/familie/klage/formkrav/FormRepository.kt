package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository

import java.util.UUID

@Repository
interface FormRepository : RepositoryInterface<Form, UUID>, InsertUpdateRepository<Form> {
}