package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.exception.feilHvisIkke
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakPersonService(
    private val fagsakPersonRepository: FagsakPersonRepository,
) {
    fun hentIdenter(personId: UUID): Set<PersonIdent> {
        val personIdenter = fagsakPersonRepository.findPersonIdenter(personId)
        feilHvis(personIdenter.isEmpty()) { "Finner ikke personidenter til person=$personId" }
        return personIdenter
    }

    fun hentAktivIdent(personId: UUID): String = fagsakPersonRepository.hentAktivIdent(personId)

    @Transactional
    fun hentEllerOpprettPerson(
        personIdenter: Set<String>,
        gjeldendePersonIdent: String,
    ): FagsakPerson {
        feilHvisIkke(personIdenter.contains(gjeldendePersonIdent)) {
            "Liste med personidenter inneholder ikke gjeldende personident"
        }
        return (
            fagsakPersonRepository.findByIdent(personIdenter)
                ?: fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(gjeldendePersonIdent))))
        )
    }

    @Transactional
    fun oppdaterIdent(
        fagsakPerson: FagsakPerson,
        gjeldendePersonIdent: String,
    ): FagsakPerson =
        if (fagsakPerson.hentAktivIdent() != gjeldendePersonIdent) {
            fagsakPersonRepository.update(fagsakPerson.medOppdatertGjeldendeIdent(gjeldendePersonIdent))
        } else {
            fagsakPerson
        }
}
