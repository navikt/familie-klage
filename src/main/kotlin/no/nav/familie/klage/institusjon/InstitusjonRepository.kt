package no.nav.familie.klage.institusjon

import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID
import org.springframework.data.jdbc.repository.query.Query

@Repository
interface InstitusjonRepository :
    RepositoryInterface<Institusjon, UUID>,
    InsertUpdateRepository<Institusjon> {
    @Query("select * from Institusjon i where i.org_nummer = :orgNummer")
    fun finnInstitusjon(orgNummer: String): Institusjon?
}
