package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.behandling.BehandlingsRepository
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(
    private val personopplysningerRepository: PersonopplysningerRepository,
    private val behandlingsRepository: BehandlingsRepository
    ) {

    fun hentPersonopplysninger(behandlingId: UUID): Personopplysninger {
        val behandling: Behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        return personopplysningerRepository.findByPersonId(behandling.personId)
    }

    fun opprettPersonopplysninger(personopplysninger: Personopplysninger): Personopplysninger {
        return personopplysningerRepository.insert(
            Personopplysninger(
                personId = personopplysninger.personId,
                navn = personopplysninger.navn,
                kjønn = personopplysninger.kjønn,
                telefonnummer = personopplysninger.telefonnummer,
                adresse = personopplysninger.adresse
            )
        )
    }

    fun hentNavn(personId: String): String{ // TODO legg til slik at fornavn og etternavn hentes når db er oppdatert til navn-objekt
        return personopplysningerRepository.findNavnByPersonId(personId)
    }
}
