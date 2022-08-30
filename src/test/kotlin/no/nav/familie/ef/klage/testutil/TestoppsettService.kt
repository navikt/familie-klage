package no.nav.familie.ef.klage.testutil

import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository
) {

    fun opprettPerson(ident: String) = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        return fagsakRepository.insert(
            FagsakDomain(
                id = fagsak.id,
                fagsakPersonId = person.id,
                stønadstype = fagsak.stønadstype,
                fagsystem = fagsak.fagsystem,
                eksternId = fagsak.eksternId,
                sporbar = fagsak.sporbar
            )
        ).tilFagsakMedPerson(person.identer)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson {
        val person = fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
        return person ?: fagsakPersonRepository.insert(
            FagsakPerson(
                fagsak.fagsakPersonId,
                identer = fagsak.personIdenter
            )
        )
    }
}
