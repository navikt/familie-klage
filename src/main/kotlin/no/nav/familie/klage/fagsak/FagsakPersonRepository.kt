package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakPersonRepository : RepositoryInterface<FagsakPerson, UUID>, InsertUpdateRepository<FagsakPerson> {
    @Query(
        """SELECT p.* FROM fagsak_person p WHERE 
                EXISTS(SELECT 1 FROM person_ident WHERE fagsak_person_id = p.id AND ident IN (:identer))"""
    )
    fun findByIdent(identer: Collection<String>): FagsakPerson?

    @Query("SELECT * FROM person_ident WHERE fagsak_person_id = :personId")
    fun findPersonIdenter(personId: UUID): Set<PersonIdent>

    @Query("SELECT ident FROM person_ident WHERE fagsak_person_id = :personId ORDER BY endret_tid DESC LIMIT 1")
    fun hentAktivIdent(personId: UUID): String
}
