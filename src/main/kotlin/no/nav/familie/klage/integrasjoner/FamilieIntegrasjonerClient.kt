package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.felles.util.medContentTypeJsonUTF8
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.log.NavHttpHeaders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class FamilieIntegrasjonerClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    @Value("\${FAMILIE_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    private val integrasjonerConfig: IntegrasjonerConfig,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val dokuarkivUri: URI =
        UriComponentsBuilder
            .fromUri(integrasjonUri)
            .pathSegment("api/arkiv")
            .build()
            .toUri()
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val saksbehandlerUri: URI = integrasjonerConfig.saksbehandlerUri

    // lagre brev
    fun arkiverDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String?,
    ): ArkiverDokumentResponse =
        restClient
            .post()
            .uri(URI.create("$dokuarkivUri/v4"))
            .contentType(MediaType.APPLICATION_JSON)
            .headers { headerMedSaksbehandler(saksbehandler, it) }
            .body(arkiverDokumentRequest)
            .retrieve()
            .body<Ressurs<ArkiverDokumentResponse>>()!!
            .data
            ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler =
        restClient
            .get()
            .uri(URI.create("$saksbehandlerUri/$navIdent"))
            .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
            .retrieve()
            .body<Ressurs<Saksbehandler>>()!!
            .data
            ?: error("Kunne ikke hente saksbehandlerinfo for saksbehandler med ident=$navIdent")

    // sende brev til bruker
    fun distribuerBrev(
        journalpostId: String,
        distribusjonstype: Distribusjonstype,
    ): String {
        val journalpostRequest =
            DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                bestillendeFagsystem = Fagsystem.EF,
                dokumentProdApp = "FAMILIE_KLAGE",
                distribusjonstype = distribusjonstype,
            )
        return restClient
            .post()
            .uri(integrasjonerConfig.distribuerDokumentUri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(journalpostRequest)
            .retrieve()
            .body<Ressurs<String>>()!!
            .getDataOrThrow()
    }

    // sende brev til bruker
    fun distribuerBrev(
        journalpostId: String,
        distribusjonstype: Distribusjonstype,
        adresse: ManuellAdresse?,
        fagsystem: Fagsystem,
    ): String {
        val journalpostRequest =
            DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                bestillendeFagsystem = fagsystem,
                dokumentProdApp = "FAMILIE_KLAGE",
                distribusjonstype = distribusjonstype,
                adresse = adresse,
            )
        return restClient
            .post()
            .uri(integrasjonerConfig.distribuerDokumentUri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(journalpostRequest)
            .retrieve()
            .body<Ressurs<String>>()!!
            .getDataOrThrow()
    }

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> =
        restClient
            .post()
            .uri(journalpostURI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(journalposterForBrukerRequest)
            .retrieve()
            .body<Ressurs<List<Journalpost>>>()!!
            .data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerRequest.brukerId.id}")

    fun hentJournalpost(journalpostId: String): Journalpost {
        try {
            return restClient
                .get()
                .uri(URI.create("$journalpostURI?journalpostId=$journalpostId"))
                .retrieve()
                .body<Ressurs<Journalpost>>()!!
                .getDataOrThrow()
        } catch (e: HttpClientErrorException) {
            if (e.responseBodyAsString.contains("Fant ikke journalpost i fagarkivet")) {
                throw ApiFeil("Finner ikke journalpost i fagarkivet", HttpStatus.BAD_REQUEST)
            } else {
                throw e
            }
        }
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray =
        restClient
            .get()
            .uri(
                UriComponentsBuilder
                    .fromUriString(
                        "$journalpostURI/hentdokument/" +
                            "$journalpostId/$dokumentInfoId",
                    ).queryParam("variantFormat", Dokumentvariantformat.ARKIV)
                    .build()
                    .toUri(),
            ).retrieve()
            .body<Ressurs<ByteArray>>()!!
            .getDataOrThrow()

    fun hentOrganisasjon(orgNummer: String): Organisasjon {
        val uri =
            UriComponentsBuilder
                .fromUri(integrasjonerConfig.hentOrganisasjonUri)
                .pathSegment(orgNummer)
                .build()
                .toUri()
        return restClient
            .get()
            .uri(uri)
            .retrieve()
            .body<Ressurs<Organisasjon>>()!!
            .getDataOrThrow()
    }

    private fun headerMedSaksbehandler(
        saksbehandler: String?,
        headers: HttpHeaders,
    ) {
        if (saksbehandler != null) {
            headers.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
    }
}
