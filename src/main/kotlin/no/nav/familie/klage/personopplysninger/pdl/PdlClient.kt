package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.config.PdlConfig
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class PdlClient(
    val pdlConfig: PdlConfig,
    @Qualifier("pdlRestClient") private val restClient: RestClient,
) {
    @Cacheable("hentPerson", cacheManager = "shortCache")
    fun hentPerson(
        personIdent: String,
        stønadstype: Stønadstype,
    ): PdlPerson {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = PdlConfig.hentPersonQuery,
            )
        val pdlResponse: PdlResponse<PdlPersonData> =
            restClient
                .post()
                .uri(pdlConfig.pdlUri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.addAll(httpHeaders(mapTilTema(stønadstype))) }
                .body(pdlPersonRequest)
                .retrieve()
                .body<PdlResponse<PdlPersonData>>()!!
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
            restClient
                .post()
                .uri(pdlConfig.pdlUri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.addAll(httpHeaders(mapTilTema(stønadstype))) }
                .body(pdlPersonRequest)
                .retrieve()
                .body<PdlBolkResponse<PdlNavn>>()!!
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
            restClient
                .post()
                .uri(pdlConfig.pdlUri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.addAll(httpHeaders(tema)) }
                .body(pdlIdentRequest)
                .retrieve()
                .body<PdlResponse<PdlHentIdenter>>()!!
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    fun hentPersonidenter(
        ident: String,
        stønadstype: Stønadstype,
        historikk: Boolean = false,
    ): PdlIdenter = hentPersonidenter(ident, mapTilTema(stønadstype), historikk)

    private fun httpHeaders(tema: Tema): org.springframework.http.HttpHeaders =
        org.springframework.http.HttpHeaders().apply {
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
