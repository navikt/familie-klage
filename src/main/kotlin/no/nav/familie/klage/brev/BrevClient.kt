package no.nav.familie.klage.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.klage.blankett.BlankettPdfRequest
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.felles.util.TekstUtil.norskFormat
import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.restklient.client.AbstractPingableRestClient
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

    fun genererHtmlFritekstbrev(
        fritekstBrev: FritekstBrevRequestDto,
        saksbehandlerNavn: String,
        enhet: String,
        fagsystem: Fagsystem,
        brevmottakere: Brevmottakere?,
    ): String {
        val url =
            if (fagsystem in setOf(Fagsystem.BA, Fagsystem.KS)) {
                URI.create("$familieBrevUri/api/fritekst-brev/baks/html")
            } else {
                URI.create("$familieBrevUri/api/fritekst-brev/html")
            }
        return postForEntity(
            url,
            FritekstBrevRequestMedSignatur(
                brevFraSaksbehandler = fritekstBrev,
                saksbehandlersignatur = saksbehandlerNavn,
                enhet = enhet,
                brevmottakere = brevmottakere,
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray = postForEntity(pdfUri, blankettPdfRequest, HttpHeaders().medContentTypeJsonUTF8())

    fun genererHtml(
        brevmal: String,
        saksbehandlerBrevrequest: JsonNode,
        saksbehandlersignatur: String,
        saksbehandlerEnhet: String?,
        skjulBeslutterSignatur: Boolean,
        stønadstype: Stønadstype,
    ): String {
        feilHvis(brevmal === FRITEKST) {
            "HTML-generering av fritekstbrev er ikke implementert"
        }

        val datasetOgType = hentTilhørendeSanityDatasetOgType(stønadstype)
        val url = URI.create("$familieBrevUri/api/$datasetOgType/bokmaal/$brevmal/html")

        return postForEntity(
            url,
            BrevRequest(
                brevFraSaksbehandler = saksbehandlerBrevrequest,
                saksbehandlersignatur = saksbehandlersignatur,
                saksbehandlerEnhet = saksbehandlerEnhet,
                skjulBeslutterSignatur = skjulBeslutterSignatur,
                dato = LocalDate.now().norskFormat(),
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )
    }

    private fun hentTilhørendeSanityDatasetOgType(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETRYGD -> BA_SANITY_DATASET
            Stønadstype.KONTANTSTØTTE -> KS_SANITY_DATASET
            Stønadstype.OVERGANGSSTØNAD, Stønadstype.BARNETILSYN, Stønadstype.SKOLEPENGER -> EF_SANITTY_DATASET
        }

    companion object {
        const val FRITEKST = "fritekst"

        const val BA_SANITY_DATASET = "ba-brev/dokument"
        const val KS_SANITY_DATASET = "ks-brev/dokument"
        const val EF_SANITTY_DATASET = "ef-brev/avansert-dokument"
    }
}

data class FritekstBrevRequestMedSignatur(
    val brevFraSaksbehandler: FritekstBrevRequestDto,
    val saksbehandlersignatur: String,
    val enhet: String,
    val brevmottakere: Brevmottakere? = null,
)

data class BrevRequest(
    val brevFraSaksbehandler: JsonNode,
    val saksbehandlersignatur: String,
    val saksbehandlerEnhet: String? = null,
    val besluttersignatur: String? = null,
    val beslutterEnhet: String? = null,
    val skjulBeslutterSignatur: Boolean,
    val dato: String,
)
