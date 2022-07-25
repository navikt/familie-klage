package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.behandling.BehandlingsRepository
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(
    private val personopplysningerRepository: PersonopplysningerRepository,
    private val behandlingsRepository: BehandlingsRepository,
    private val fagsakService: FagsakService,
    ) {

    fun hentPersonopplysninger(behandlingId: UUID): Personopplysninger {
        val behandling = behandlingsRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        return personopplysningerRepository.findByPersonId(fagsak.personId)
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
        return personopplysningerRepository.findByPersonId(personId).navn
    }
}
