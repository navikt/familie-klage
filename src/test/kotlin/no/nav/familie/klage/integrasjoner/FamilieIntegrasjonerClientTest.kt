package no.nav.familie.klage.integrasjoner

import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import org.springframework.http.HttpMethod as SpringHttpMethod

class FamilieIntegrasjonerClientTest {
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val baseUrl = "http://localhost:8080"

    private val client =
        FamilieIntegrasjonerClient(
            restClient = restClientBuilder.build(),
            integrasjonUri = URI.create(baseUrl),
            integrasjonerConfig = IntegrasjonerConfig(URI.create(baseUrl)),
        )

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
