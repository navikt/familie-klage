package no.nav.familie.klage.blankett

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class BlankettClient(
    @Value("\${FAMILIE_BLANKETT_API_URL}")
    private val familieBlankettUri: String,
    @Qualifier("utenAuth")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(
    restOperations,
    "familie.blankett",
) {

    private val pdfUrl = URI.create("$familieBlankettUri/api/klage/pdf")

    override val pingUri: URI = pdfUrl

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray {
        return postForEntity(pdfUrl, blankettPdfRequest, HttpHeaders().medContentTypeJsonUTF8())
    }
}
