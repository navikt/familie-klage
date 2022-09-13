package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
import java.util.UUID

interface FagsakRepository : RepositoryInterface<FagsakDomain, UUID>, InsertUpdateRepository<FagsakDomain> {

    fun findByEksternIdAndFagsystemAndStønadstype(
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype
    ): FagsakDomain?

    @Query(
        """SELECT f.*
                    FROM fagsak f
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId"""
    )
    fun finnFagsakForBehandlingId(behandlingId: UUID): FagsakDomain?
}
