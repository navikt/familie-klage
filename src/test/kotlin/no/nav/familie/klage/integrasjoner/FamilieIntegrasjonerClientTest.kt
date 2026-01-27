package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI

class FamilieIntegrasjonerClientTest {
    private val restOperations = mockk<RestOperations>()
    private val baseUrl = "http://localhost:8080"

    private val client =
        FamilieIntegrasjonerClient(
            restOperations = restOperations,
            integrasjonUri = URI.create(baseUrl),
            integrasjonerConfig = IntegrasjonerConfig(URI.create(baseUrl)),
        )

    @Nested
    inner class HentOrganisasjon {
        @Test
        fun `skal hente organisasjon`() {
            // Arrange
            val orgNummer = "123456789"

            val uri = URI.create("$baseUrl/api/organisasjon/$orgNummer")

            every {
                restOperations.exchange<Ressurs<Organisasjon>>(
                    url = any<URI>(),
                    method = any<HttpMethod>(),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns
                ResponseEntity.ok(
                    Ressurs.success(
                        Organisasjon(
                            navn = "navn",
                            organisasjonsnummer = orgNummer,
                        ),
                    ),
                )

            // Act
            val organisasjon = client.hentOrganisasjon(orgNummer = orgNummer)

            // Assert
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<Organisasjon>>(
                    eq(uri),
                    eq(HttpMethod.GET),
                    any<HttpEntity<Void>>(),
                )
            }
            assertThat(organisasjon.organisasjonsnummer).isEqualTo(orgNummer)
            assertThat(organisasjon.navn).isEqualTo("navn")
            assertThat(organisasjon.adresse).isNull()
        }

        @Test
        fun `skal kaste feil om ressurs er failure`() {
            // Arrange
            val orgNummer = "123456789"

            val uri = URI.create("$baseUrl/api/organisasjon/$orgNummer")

            every {
                restOperations.exchange<Ressurs<Organisasjon>>(
                    url = any<URI>(),
                    method = any<HttpMethod>(),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns
                ResponseEntity.ok(
                    Ressurs.failure(
                        errorMessage = "Ops! En ukjent feil oppstod.",
                    ),
                )

            // Act & assert
            val exception = assertThrows<IllegalStateException> { client.hentOrganisasjon(orgNummer = orgNummer) }
            assertThat(exception.message).isEqualTo("Ops! En ukjent feil oppstod.")
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<Organisasjon>>(
                    eq(uri),
                    eq(HttpMethod.GET),
                    any<HttpEntity<Void>>(),
                )
            }
        }
    }
}
