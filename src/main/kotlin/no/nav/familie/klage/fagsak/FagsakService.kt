package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakService (
    private val fagsakRepository: FagsakRepository
    ){
    fun opprettFagsak(fagsak: Fagsak): Fagsak {
        return fagsakRepository.insert(
            Fagsak(
                id = fagsak.id,
                personIdent = fagsak.personIdent,
                stønadsType = fagsak.stønadsType
            )
        )
    }

    fun hentFagsak(id: UUID): Fagsak{
        return fagsakRepository.findByIdOrThrow(id)
    }
}