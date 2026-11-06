package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.net.URI
import org.springframework.http.HttpMethod as SpringHttpMethod

class FamilieIntegrasjonerClientTest {
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val restClientGcpBuilder = RestClient.builder()
    private val mockServerGcp = MockRestServiceServer.bindTo(restClientGcpBuilder).build()
    private val baseUrl = "http://localhost:8080"
    private val baseUrlGcp = "http://localhost:8081"
    private val featureToggleService =
        mockk<FeatureToggleService> {
            every { isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns false
        }

    private val client =
        FamilieIntegrasjonerClient(
            IntegrasjonerClientProvider(
                restClientFss = restClientBuilder.defaultHeader("X-Test-Cluster", "fss").build(),
                integrasjonFssUri = URI.create(baseUrl),
                restClientGcp = restClientGcpBuilder.defaultHeader("X-Test-Cluster", "gcp").build(),
                integrasjonGcpUri = URI.create(baseUrlGcp),
                featureToggleService = featureToggleService,
            ),
        )

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal velge URL og klient fra samme cluster`(brukGcp: Boolean) {
        every { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns brukGcp
        val server = if (brukGcp) mockServerGcp else mockServer
        val url = if (brukGcp) baseUrlGcp else baseUrl
        val cluster = if (brukGcp) "gcp" else "fss"
        server
            .expect(requestTo("$url/api/organisasjon/123456789"))
            .andExpect(header("X-Test-Cluster", cluster))
            .andRespond(
                withSuccess(
                    jsonMapper.writeValueAsString(Ressurs.success(Organisasjon(navn = "navn", organisasjonsnummer = "123456789"))),
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThat(client.hentOrganisasjon("123456789").organisasjonsnummer).isEqualTo("123456789")

        mockServer.verify()
        mockServerGcp.verify()
        verify(exactly = 1) { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal rute begge distribuerBrev-varianter til valgt cluster`(brukGcp: Boolean) {
        every { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns brukGcp
        val server = if (brukGcp) mockServerGcp else mockServer
        val url = if (brukGcp) baseUrlGcp else baseUrl
        val cluster = if (brukGcp) "gcp" else "fss"
        repeat(2) {
            server
                .expect(requestTo("$url/api/dist/v1"))
                .andExpect(method(SpringHttpMethod.POST))
                .andExpect(header("X-Test-Cluster", cluster))
                .andRespond(withSuccess(jsonMapper.writeValueAsString(Ressurs.success("123")), MediaType.APPLICATION_JSON))
        }

        assertThat(client.distribuerBrev("1234", Distribusjonstype.VEDTAK)).isEqualTo("123")
        assertThat(client.distribuerBrev("1234", Distribusjonstype.VEDTAK, null, Fagsystem.BA)).isEqualTo("123")

        mockServer.verify()
        mockServerGcp.verify()
        verify(exactly = 2) { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) }
    }

    @Test
    fun `skal kunne bytte til GCP og tilbake til FSS uten omstart`() {
        every { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returnsMany listOf(false, true, false)
        repeat(2) {
            mockServer
                .expect(requestTo("$baseUrl/api/journalpost/hentdokument/1234/5678?variantFormat=ARKIV"))
                .andExpect(header("X-Test-Cluster", "fss"))
                .andRespond(withSuccess(jsonMapper.writeValueAsString(Ressurs.success(byteArrayOf(1, 2))), MediaType.APPLICATION_JSON))
        }
        mockServerGcp
            .expect(requestTo("$baseUrlGcp/api/journalpost/hentdokument/1234/5678?variantFormat=ARKIV"))
            .andExpect(header("X-Test-Cluster", "gcp"))
            .andRespond(withSuccess(jsonMapper.writeValueAsString(Ressurs.success(byteArrayOf(1, 2))), MediaType.APPLICATION_JSON))

        repeat(3) {
            assertThat(client.hentDokument("1234", "5678")).containsExactly(1, 2)
        }

        mockServer.verify()
        mockServerGcp.verify()
        verify(exactly = 3) { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) }
    }

    @Test
    fun `skal ikke falle tilbake til FSS når et kall til GCP feiler`() {
        every { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns true
        mockServerGcp
            .expect(requestTo("$baseUrlGcp/api/journalpost?journalpostId=1234"))
            .andExpect(header("X-Test-Cluster", "gcp"))
            .andRespond(withServerError())

        assertThrows<HttpServerErrorException> { client.hentJournalpost("1234") }

        mockServer.verify()
        mockServerGcp.verify()
        verify(exactly = 1) { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) }
    }

    @Nested
    inner class HentOrganisasjon {
        @Test
        fun `skal hente organisasjon`() {
            // Arrange
            val orgNummer = "123456789"
            val uri = "$baseUrl/api/organisasjon/$orgNummer"

            mockServer
                .expect(requestTo(uri))
                .andExpect(method(SpringHttpMethod.GET))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(
                            Ressurs.success(Organisasjon(navn = "navn", organisasjonsnummer = orgNummer)),
                        ),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            // Act
            val organisasjon = client.hentOrganisasjon(orgNummer = orgNummer)

            // Assert
            mockServer.verify()
            assertThat(organisasjon.organisasjonsnummer).isEqualTo(orgNummer)
            assertThat(organisasjon.navn).isEqualTo("navn")
            assertThat(organisasjon.adresse).isNull()
        }

        @Test
        fun `skal kaste feil om ressurs er failure`() {
            // Arrange
            val orgNummer = "123456789"
            val uri = "$baseUrl/api/organisasjon/$orgNummer"

            mockServer
                .expect(requestTo(uri))
                .andExpect(method(SpringHttpMethod.GET))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.failure<Organisasjon>(errorMessage = "Ops! En ukjent feil oppstod.")),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            // Act & assert
            val exception = assertThrows<IllegalStateException> { client.hentOrganisasjon(orgNummer = orgNummer) }
            assertThat(exception.message).isEqualTo("Ops! En ukjent feil oppstod.")
        }
    }
}
