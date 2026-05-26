package no.nav.familie.klage.brev

import no.nav.familie.klage.blankett.BlankettPdfRequest
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.felles.util.TekstUtil.norskFormat
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import tools.jackson.databind.JsonNode
import java.net.URI
import java.time.LocalDate

@Component
class BrevClient(
    @Value("\${FAMILIE_BREV_API_URL}")
    private val familieBrevUri: String,
    @Qualifier("utenAuthRestClient")
    private val restClient: RestClient,
) {
    fun genererHtmlFritekstbrev(
        fritekstBrev: FritekstBrevRequestDto,
        saksbehandlerNavn: String?,
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
        return restClient
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                FritekstBrevRequestMedSignatur(
                    brevFraSaksbehandler = fritekstBrev,
                    saksbehandlersignatur = saksbehandlerNavn,
                    enhet = enhet,
                    brevmottakere = brevmottakere,
                ),
            ).retrieve()
            .body<String>()!!
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray =
        restClient
            .post()
            .uri(URI.create("$familieBrevUri/blankett/klage/pdf"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(blankettPdfRequest)
            .retrieve()
            .body<ByteArray>()!!

    fun genererHtml(
        brevmal: String,
        saksbehandlerBrevrequest: JsonNode,
        saksbehandlersignatur: String?,
        saksbehandlerEnhet: String?,
        skjulBeslutterSignatur: Boolean,
        stønadstype: Stønadstype,
    ): String {
        feilHvis(brevmal === FRITEKST) {
            "HTML-generering av fritekstbrev er ikke implementert"
        }

        val datasetOgType = hentTilhørendeSanityDatasetOgType(stønadstype)
        val url = URI.create("$familieBrevUri/api/$datasetOgType/bokmaal/$brevmal/html")

        return restClient
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                BrevRequest(
                    brevFraSaksbehandler = saksbehandlerBrevrequest,
                    saksbehandlersignatur = saksbehandlersignatur,
                    saksbehandlerEnhet = saksbehandlerEnhet,
                    skjulBeslutterSignatur = skjulBeslutterSignatur,
                    dato = LocalDate.now().norskFormat(),
                ),
            ).retrieve()
            .body<String>()!!
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
    val saksbehandlersignatur: String? = null,
    val enhet: String,
    val brevmottakere: Brevmottakere? = null,
)

data class BrevRequest(
    val brevFraSaksbehandler: JsonNode,
    val saksbehandlersignatur: String? = null,
    val saksbehandlerEnhet: String? = null,
    val besluttersignatur: String? = null,
    val beslutterEnhet: String? = null,
    val skjulBeslutterSignatur: Boolean,
    val dato: String,
)
