package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.infrastruktur.config.PdlConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(
    val pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") restTemplate: RestOperations
) :
    AbstractPingableRestClient(restTemplate, "pdl.personinfo") {

    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun hentPerson(personIdent: String): PdlSøker {
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = PdlConfig.søkerQuery
        )
        val pdlResponse: PdlResponse<PdlSøkerData> = postForEntity(
            pdlConfig.pdlUri,
            pdlPersonRequest,
            httpHeaders()
        )
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @param historikk default false, tar med historikk hvis det er ønskelig
     * @return liste med folkeregisteridenter
     */
    @Cacheable("personidenter", cacheManager = "shortCache")
    fun hentPersonidenter(ident: String, historikk: Boolean = false): PdlIdenter {
        val pdlIdentRequest = PdlIdentRequest(
            variables = PdlIdentRequestVariables(ident, "FOLKEREGISTERIDENT", historikk),
            query = PdlConfig.hentIdentQuery
        )
        val pdlResponse: PdlResponse<PdlHentIdenter> = postForEntity(
            pdlConfig.pdlUri,
            pdlIdentRequest,
            httpHeaders()
        )
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    private fun httpHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            add("Tema", "ENF")
        }
    }

}
