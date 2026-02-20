package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.config.PdlConfig
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.restklient.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(
    val pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") restTemplate: RestOperations,
) : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {
    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    @Cacheable("hentPerson", cacheManager = "shortCache")
    fun hentPerson(
        personIdent: String,
        stønadstype: Stønadstype,
    ): PdlSøker {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = PdlConfig.søkerQuery,
            )
        val pdlResponse: PdlResponse<PdlSøkerData> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(mapTilTema(stønadstype)),
            )
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
    }

    @Cacheable("hentNavnBolk", cacheManager = "shortCache")
    fun hentNavnBolk(
        personIdenter: List<String>,
        stønadstype: Stønadstype,
    ): Map<String, PdlNavn> {
        require(personIdenter.size <= 100) { "Liste med personidenter må være færre enn 100 st" }
        val pdlPersonRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.bolkNavnQuery,
            )
        val pdlResponse: PdlBolkResponse<PdlNavn> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(mapTilTema(stønadstype)),
            )
        return feilsjekkOgReturnerData(pdlResponse)
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @param historikk default false, tar med historikk hvis det er ønskelig
     * @return liste med folkeregisteridenter
     */
    @Cacheable("personidenter", cacheManager = "shortCache")
    fun hentPersonidenter(
        ident: String,
        tema: Tema,
        historikk: Boolean = false,
    ): PdlIdenter {
        val pdlIdentRequest =
            PdlIdentRequest(
                variables = PdlIdentRequestVariables(ident, "FOLKEREGISTERIDENT", historikk),
                query = PdlConfig.hentIdentQuery,
            )
        val pdlResponse: PdlResponse<PdlHentIdenter> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlIdentRequest,
                httpHeaders(tema),
            )
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    fun hentPersonidenter(
        ident: String,
        stønadstype: Stønadstype,
        historikk: Boolean = false,
    ): PdlIdenter = hentPersonidenter(ident, mapTilTema(stønadstype), historikk)

    private fun httpHeaders(tema: Tema): HttpHeaders =
        HttpHeaders().apply {
            add("Tema", tema.name)
            add("behandlingsnummer", tema.behandlingsnummer)
        }

    private fun mapTilTema(stønadstype: Stønadstype): Tema =
        when (stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> Tema.ENF
            Stønadstype.SKOLEPENGER -> Tema.ENF
            Stønadstype.BARNETILSYN -> Tema.ENF
            Stønadstype.BARNETRYGD -> Tema.BAR
            Stønadstype.KONTANTSTØTTE -> Tema.KON
        }
}
