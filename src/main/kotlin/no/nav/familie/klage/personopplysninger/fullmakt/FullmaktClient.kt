package no.nav.familie.klage.personopplysninger.fullmakt

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.LocalDate

@Component
class FullmaktClient(
    @Value("\${REPR_API_URL}")
    private val fullmaktUrl: String,
    @Qualifier("reprApiRestClient")
    private val restClient: RestClient,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentFullmakt(ident: String): List<FullmaktResponse> {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmakt/fullmaktsgiver")
        val fullmaktResponse =
            restClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(FullmaktRequest(ident))
                .retrieve()
                .body<List<FullmaktResponse>>()!!
        secureLogger.info("FullmaktResponse: $fullmaktResponse")
        return fullmaktResponse
    }
}

data class FullmaktRequest(
    val ident: String,
)

data class FullmaktResponse(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val fullmektig: String,
    val fullmektigsNavn: String?,
    val omraade: List<Område>,
)

data class Område(
    val tema: String,
    val handling: List<Handling>,
)

@Suppress("unused")
enum class Handling {
    LES,
    KOMMUNISER,
    SKRIV,
}
