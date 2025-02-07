package no.nav.familie.klage.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.blankett.BlankettPdfRequest
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.felles.util.TekstUtil.norskFormat
import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
class BrevClient(
    @Value("\${FAMILIE_BREV_API_URL}")
    private val familieBrevUri: String,
    @Qualifier("utenAuth")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "familie.brev") {

    override val pingUri: URI = URI.create("$familieBrevUri/api/status")
    private val pdfUri = URI.create("$familieBrevUri/blankett/klage/pdf")

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererHtmlFritekstbrev(fritekstBrev: FritekstBrevRequestDto, saksbehandlerNavn: String, enhet: String): String {
        val url = URI.create("$familieBrevUri/api/fritekst-brev/html")
        return postForEntity(
            url,
            FritekstBrevRequestMedSignatur(
                fritekstBrev,
                saksbehandlerNavn,
                enhet,
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray {
        return postForEntity(pdfUri, blankettPdfRequest, HttpHeaders().medContentTypeJsonUTF8())
    }

    fun genererHtml(
        brevmal: String,
        saksbehandlerBrevrequest: JsonNode,
        saksbehandlersignatur: String,
        enhet: String?,
        skjulBeslutterSignatur: Boolean,
    ): String {
        feilHvis(brevmal === FRITEKST) {
            "HTML-generering av fritekstbrev er ikke implementert"
        }

        val url = URI.create("$familieBrevUri/api/ef-brev/avansert-dokument/bokmaal/$brevmal/html")

        return postForEntity(
            url,
            BrevRequest(
                brevFraSaksbehandler = saksbehandlerBrevrequest,
                saksbehandlersignatur = saksbehandlersignatur,
                enhet = enhet,
                skjulBeslutterSignatur = skjulBeslutterSignatur,
                dato = LocalDate.now().norskFormat(),
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        postForEntity<Any>(URI.create("http://familie-ef-iverksett/api/brev/frittstaende"), frittståendeBrevDto)
    }
}

data class FritekstBrevRequestMedSignatur(
    val brevFraSaksbehandler: FritekstBrevRequestDto,
    val saksbehandlersignatur: String,
    val enhet: String,
)

data class BrevRequest(
    val brevFraSaksbehandler: JsonNode,
    val saksbehandlersignatur: String,
    val enhet: String?,
    val skjulBeslutterSignatur: Boolean,
    val dato: String,
)

const val FRITEKST = "fritekst"
