package no.nav.familie.klage.brev

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Component
class FamilieDokumentClient(
    @Value("\${FAMILIE_DOKUMENT_URL}")
    private val familieDokumentUrl: String,
    @Qualifier("utenAuthRestClient")
    private val restClient: RestClient,
) {
    fun genererPdfFraHtml(html: String): ByteArray {
        val htmlTilPdfURI = URI.create("$familieDokumentUrl/$HTML_TIL_PDF")
        return restClient
            .post()
            .uri(htmlTilPdfURI)
            .contentType(MediaType.TEXT_HTML)
            .accept(MediaType.APPLICATION_PDF)
            .body(html.encodeToByteArray())
            .retrieve()
            .body<ByteArray>()!!
    }

    companion object {
        const val HTML_TIL_PDF = "api/html-til-pdf"
    }
}
