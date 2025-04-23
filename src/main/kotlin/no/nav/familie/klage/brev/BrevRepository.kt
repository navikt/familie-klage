package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BrevRepository :
    RepositoryInterface<Brev, UUID>,
    InsertUpdateRepository<Brev> {
    @Modifying
    @Query("""UPDATE brev SET mottakere_journalposter = :brevmottakereJournalposter WHERE behandling_id = :behandlingId""")
    fun oppdaterMottakerJournalpost(
        behandlingId: UUID,
        brevmottakereJournalposter: BrevmottakereJournalposter,
    )
}
