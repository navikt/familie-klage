package no.nav.familie.klage.integrasjoner

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.log.NavHttpHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieIntegrasjonerClient (
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    ): AbstractPingableRestClient(restOperations, "journalpost") {

    override val pingUri: URI = URI.create("$integrasjonUri/api/status")
    private val dokuarkivUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/arkiv").build().toUri()
    private val distribuerDokumentUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/dist/v1").build().toUri()

    //lagre brev
    fun arkiverDokument(arkiverDokumentRequest: ArkiverDokumentRequest, saksbehandler: String?): ArkiverDokumentResponse {
        return postForEntity<Ressurs<ArkiverDokumentResponse>>(
            URI.create("$dokuarkivUri/v4/"),
            arkiverDokumentRequest,
            headerMedSaksbehandler(saksbehandler)
        ).data
            ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")

    }

    //sende brev til bruker
    fun distribuerBrev(journalpostId: String, distribusjonstype: Distribusjonstype): String{
        val journalpostRequest = DistribuerJournalpostRequest(
            journalpostId = journalpostId,
            bestillendeFagsystem = Fagsystem.EF,
            dokumentProdApp = "FAMILIE_KLAGE",
            distribusjonstype = distribusjonstype
        )

        return postForEntity<Ressurs<String>>(
            distribuerDokumentUri,
            journalpostRequest,
            HttpHeaders().medContentTypeJsonUTF8()
        ).getDataOrThrow()
    }

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders{
        val httpHeaders = HttpHeaders()
        if(saksbehandler != null){
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }
}