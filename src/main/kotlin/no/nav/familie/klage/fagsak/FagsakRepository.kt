package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.Stønadstype
import no.nav.familie.klage.repository.InsertUpdateRepository
import no.nav.familie.klage.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.Fagsystem
import java.util.UUID

interface FagsakRepository : RepositoryInterface<FagsakDomain, UUID>, InsertUpdateRepository<FagsakDomain> {

    fun findByEksternIdAndFagsystemAndStønadstype(
        eksternId: String,
        fagsystem: Fagsystem,
        stønadstype: Stønadstype
    ): FagsakDomain?
}
