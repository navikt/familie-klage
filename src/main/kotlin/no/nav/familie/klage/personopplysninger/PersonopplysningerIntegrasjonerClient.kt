package no.nav.familie.klage.personopplysninger

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.felles.dto.EgenAnsattRequest
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.felles.dto.Tilgang
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class PersonopplysningerIntegrasjonerClient(
        @Qualifier("azure") restOperations: RestOperations,
        private val integrasjonerConfig: IntegrasjonerConfig
) :
        AbstractPingableRestClient(restOperations, "familie.integrasjoner") {

    override val pingUri: URI = integrasjonerConfig.pingUri

    fun sjekkTilgangTilPerson(personIdent: String): Tilgang {
        return postForEntity<List<Tilgang>>(
                integrasjonerConfig.tilgangPersonUri, listOf(personIdent),
                HttpHeaders().also {
                    it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
                }
        ).single()
    }

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang {
        return postForEntity(
                integrasjonerConfig.tilgangRelasjonerUri, PersonIdent(personIdent),
                HttpHeaders().also {
                    it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
                }
        )
    }

//    fun hentNavEnhetForPersonMedRelasjoner(ident: String): List<Arbeidsfordelingsenhet> {
//        val uri = integrasjonerConfig.arbeidsfordelingMedRelasjonerUri
//        return hentArbeidsfordelingEnhet(uri, ident)
//    }

    fun hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent: String): ADRESSEBESKYTTELSEGRADERING {
        return postForEntity<Ressurs<ADRESSEBESKYTTELSEGRADERING>>(
                integrasjonerConfig.adressebeskyttelse,
                PersonIdent(personIdent),
                HttpHeaders().also {
                    it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
                }
        ).getDataOrThrow()
    }

//    private fun hentArbeidsfordelingEnhet(
//        uri: URI,
//        ident: String
//    ): List<Arbeidsfordelingsenhet> {
//        return try {
//            val response = postForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, PersonIdent(ident))
//            response.data ?: throw Feil("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt uri=$uri")
//        } catch (e: RestClientException) {
//            throw Feil("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet uri=$uri", e)
//        }
//    }

    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(
                integrasjonerConfig.egenAnsattUri,
                EgenAnsattRequest(ident)
        ).data!!.erEgenAnsatt
    }

    fun hentNavKontor(ident: String): NavKontorEnhet? {
        val ressurs = postForEntity<Ressurs<NavKontorEnhet>>(integrasjonerConfig.navKontorUri, PersonIdent(ident))
        if (ressurs.status != Ressurs.Status.SUKSESS) {
            error("Henting av nav-kontor feilet status=${ressurs.status} - ${ressurs.melding}")
        }
        return ressurs.data
    }

    companion object {

        const val HEADER_NAV_TEMA = "Nav-Tema"
        const val HEADER_NAV_TEMA_ENF = "ENF"
    }
}
