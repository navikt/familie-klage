package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlPersonMedRelasjoner
import no.nav.familie.kontrakter.felles.Tema
import org.springframework.stereotype.Service

@Service
class PersonMedRelasjonerService(
    private val pdlClient: PdlClient,
) {
    /**
     * Henter identene som saksbehandler må ha tilgang til for å få tilgang til personen: personen selv, barna,
     * personer relatert via sivilstand og barnas andre foreldre.
     */
    fun hentIdenterForPersonMedRelasjoner(
        personIdent: String,
        tema: Tema,
    ): Set<String> {
        val person = pdlClient.hentPersonMedRelasjoner(personIdent, tema)
        val barnIdenter = person.barnIdenter()
        val sivilstandIdenter = person.sivilstand.mapNotNull { it.relatertVedSivilstand }.toSet()
        val barnasAndreForeldre = hentBarnasForeldre(barnIdenter, tema) - personIdent

        return setOf(personIdent) + barnIdenter + sivilstandIdenter + barnasAndreForeldre
    }

    private fun hentBarnasForeldre(
        barnIdenter: Set<String>,
        tema: Tema,
    ): Set<String> {
        if (barnIdenter.isEmpty()) return emptySet()

        val barna = pdlClient.hentPersonerMedRelasjoner(barnIdenter.toList(), tema)
        val barnUtenSvar = barnIdenter - barna.keys
        if (barnUtenSvar.isNotEmpty()) {
            throw Feil("Fikk ikke svar fra PDL for ${barnUtenSvar.size} av ${barnIdenter.size} barn ved henting av barnas foreldre.")
        }

        return barna.values
            .flatMap { barn -> barn.forelderBarnRelasjon.filterNot { it.erBarn() }.mapNotNull { it.relatertPersonsIdent } }
            .toSet()
    }

    private fun PdlPersonMedRelasjoner.barnIdenter(): Set<String> = forelderBarnRelasjon.filter { it.erBarn() }.mapNotNull { it.relatertPersonsIdent }.toSet()
}
