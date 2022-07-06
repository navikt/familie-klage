package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(private val personopplysningerRepository: PersonopplysningerRepository) {

    fun hentPersonopplysninger(id: UUID): Personopplysninger = personopplysningerRepository.findByPersonIdent(id)

    fun opprettPersonopplysninger(personopplysninger: Personopplysninger): Personopplysninger {
        return personopplysningerRepository.insert(
            Personopplysninger(
                personIdent = personopplysninger.personIdent,
                navn = personopplysninger.navn,
                kjønn = personopplysninger.kjønn,
                telefonnummer = personopplysninger.telefonnummer,
                adresse = personopplysninger.adresse
            )
        )
    }
}
