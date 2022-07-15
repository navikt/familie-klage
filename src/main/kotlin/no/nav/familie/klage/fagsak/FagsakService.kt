package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.Fagsak
import org.springframework.stereotype.Service
@Service
class FagsakService (
    private val fagsakRepository: FagsakRepository
    ){
    fun opprettFagsak(fagsak: Fagsak): Fagsak {
        return fagsakRepository.insert(
            Fagsak(
                id = fagsak.id,
                person_id = fagsak.person_id,
                søknadsType = fagsak.søknadsType
            )
        )
    }
}