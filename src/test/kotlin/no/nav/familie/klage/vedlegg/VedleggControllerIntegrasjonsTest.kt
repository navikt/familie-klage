package no.nav.familie.klage.vedlegg

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class VedleggControllerIntegrasjonsTest : OppslagSpringRunnerTest() {
    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak)

    @BeforeEach
    internal fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal hente ut all metadata om dokumenter samt ett vedlegg`() {
        val vedleggMetadataResponse = finnVedlegg(behandling.id)
        assertThat(vedleggMetadataResponse.statusCode).isEqualTo(HttpStatus.OK)
        val førsteDokumentMetadata = vedleggMetadataResponse.body?.data?.first()
        assertThat(førsteDokumentMetadata).isNotNull
        førsteDokumentMetadata ?: error("Mangler metadata til dokument")
        val dokumentSomPdfResponse = hentDokument(førsteDokumentMetadata.journalpostId, førsteDokumentMetadata.dokumentinfoId)
        assertThat(dokumentSomPdfResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(dokumentSomPdfResponse.body).isNotNull
    }

    private fun finnVedlegg(behandlingId: UUID): ResponseEntity<Ressurs<List<DokumentinfoDto>>> =
        restTemplate.exchange(
            localhost("/api/vedlegg/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )

    private fun hentDokument(
        journalpostId: String,
        dokumentinfoId: String,
    ): ResponseEntity<ByteArray> =
        restTemplate.exchange(
            localhost("/api/vedlegg/$journalpostId/dokument-pdf/$dokumentinfoId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
