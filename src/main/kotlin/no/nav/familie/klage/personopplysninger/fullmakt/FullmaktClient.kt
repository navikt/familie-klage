package no.nav.familie.klage.personopplysninger.fullmakt

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
class FullmaktClient(
    @Value("\${REPR_API_URL}")
    private val fullmaktUrl: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractRestClient(restOperations, "fullmakt") {
    fun hentFullmakt(ident: String): List<FullmaktResponse> {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmakt/fullmaktsgiver")
        val fullmaktResponse = postForEntity<List<FullmaktResponse>>(url, FullmaktRequest(ident))
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
