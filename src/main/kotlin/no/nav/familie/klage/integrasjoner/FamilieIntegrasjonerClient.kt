package no.nav.familie.klage.integrasjoner

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.klage.behandling.domain.StønadsType
import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.log.NavHttpHeaders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class FamilieIntegrasjonerClient (
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    private val integrasjonerConfig: IntegrasjonerConfig

    ): AbstractPingableRestClient(restOperations, "journalpost") {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override val pingUri: URI = URI.create("/api/ping")

    private val dokuarkivUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/arkiv").build().toUri()


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
            integrasjonerConfig.distribuerDokumentUri,
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

    fun lagArkiverDokumentRequest(
        personIdent: String,
        pdf: ByteArray,
        fagsakId: String?,
        behandlingId: UUID,
        enhet: String,
        stønadstype: StønadsType,
        dokumenttype: Dokumenttype
    ): ArkiverDokumentRequest {
        val dokument = no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument(
            pdf,
            Filtype.PDFA,
            null,
            "Brev for ${stønadstype.name.lowercase()}",
            dokumenttype
        )
        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            fagsakId = fagsakId,
            journalførendeEnhet = enhet,
            eksternReferanseId = "$behandlingId-blankett"
        )
    }
}