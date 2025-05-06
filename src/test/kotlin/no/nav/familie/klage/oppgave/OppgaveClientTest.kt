package no.nav.familie.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.enhet.BarnetrygdEnhet
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.klage.infrastruktur.exception.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
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

class OppgaveClientTest {
    private val restOperations: RestOperations = mockk()
    private val baseUrl = URI("http://localhost:8080")
    private val oppgaveClient =
        OppgaveClient(
            restOperations = restOperations,
            integrasjonerConfig = IntegrasjonerConfig(baseUrl),
        )

    @Nested
    inner class PatchEnhetPåOppgave {
        @Test
        fun `skal patche enhet på oppgave og tilbakestille tilordnet ressurs og mappe`() {
            // Arrange
            val oppgaveId = 1L
            val nyEnhet = BarnetrygdEnhet.OSLO
            val eksisterendeOppgave = Oppgave(id = oppgaveId, versjon = 1)

            every {
                restOperations.exchange<Ressurs<Oppgave>>(
                    url = any<URI>(),
                    method = eq(HttpMethod.GET),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns ResponseEntity.ok(Ressurs.Companion.success(eksisterendeOppgave))

            every {
                restOperations.exchange<Ressurs<OppgaveResponse>>(
                    url = any<URI>(),
                    method = eq(HttpMethod.PATCH),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns ResponseEntity.ok(Ressurs.Companion.success(OppgaveResponse(oppgaveId)))

            // Act
            val oppgaveResponse =
                oppgaveClient.patchEnhetPåOppgave(
                    oppgaveId = oppgaveId,
                    nyEnhet = nyEnhet,
                    fjernMappeFraOppgave = true,
                )

            // Assert
            assertThat(oppgaveResponse.oppgaveId).isEqualTo(oppgaveId)
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<OppgaveResponse>>(
                    url = eq(URI.create("$baseUrl/api/oppgave/$oppgaveId/enhet/${nyEnhet.enhetsnummer}?fjernMappeFraOppgave=true&nullstillTilordnetRessurs=true&versjon=${eksisterendeOppgave.versjon}")),
                    method = eq(HttpMethod.PATCH),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            }
        }

        @Test
        fun `skal patche enhet på oppgave uten å tilbakestille tilordnet ressurs eller mappe`() {
            // Arrange
            val oppgaveId = 1L
            val nyEnhet = BarnetrygdEnhet.OSLO
            val eksisterendeOppgave = Oppgave(id = oppgaveId, versjon = 1)

            every {
                restOperations.exchange<Ressurs<Oppgave>>(
                    url = any<URI>(),
                    method = eq(HttpMethod.GET),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns ResponseEntity.ok(Ressurs.Companion.success(eksisterendeOppgave))

            every {
                restOperations.exchange<Ressurs<OppgaveResponse>>(
                    url = any<URI>(),
                    method = eq(HttpMethod.PATCH),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns ResponseEntity.ok(Ressurs.success(OppgaveResponse(oppgaveId)))

            // Act
            val oppgaveResponse =
                oppgaveClient.patchEnhetPåOppgave(
                    oppgaveId = oppgaveId,
                    nyEnhet = nyEnhet,
                    fjernMappeFraOppgave = false,
                    nullstillTilordnetRessurs = false,
                )

            // Assert
            assertThat(oppgaveResponse.oppgaveId).isEqualTo(oppgaveId)
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<OppgaveResponse>>(
                    url = eq(URI.create("$baseUrl/api/oppgave/$oppgaveId/enhet/${nyEnhet.enhetsnummer}?fjernMappeFraOppgave=false&nullstillTilordnetRessurs=false&versjon=${eksisterendeOppgave.versjon}")),
                    method = eq(HttpMethod.PATCH),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            }
        }

        @Test
        fun `skal håndtere feil ved patching an enhet på oppgave`() {
            // Arrange
            val oppgaveId = 1L
            val nyEnhet = BarnetrygdEnhet.OSLO
            val eksisterendeOppgave = Oppgave(id = oppgaveId, versjon = 1)

            every {
                restOperations.exchange<Ressurs<Oppgave>>(
                    url = any<URI>(),
                    method = eq(HttpMethod.GET),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns ResponseEntity.ok(Ressurs.Companion.success(eksisterendeOppgave))

            every {
                restOperations.exchange<Ressurs<OppgaveResponse>>(
                    url = any<URI>(),
                    method = eq(HttpMethod.PATCH),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns ResponseEntity.badRequest().body(Ressurs.failure())

            // Act & assert
            val exception =
                assertThrows<IntegrasjonException> {
                    oppgaveClient.patchEnhetPåOppgave(
                        oppgaveId = oppgaveId,
                        nyEnhet = nyEnhet,
                        fjernMappeFraOppgave = false,
                    )
                }
            assertThat(exception.message).isEqualTo("Oppdatering av enhet på oppgave feilet.")
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<OppgaveResponse>>(
                    url = eq(URI.create("$baseUrl/api/oppgave/$oppgaveId/enhet/${nyEnhet.enhetsnummer}?fjernMappeFraOppgave=false&nullstillTilordnetRessurs=true&versjon=${eksisterendeOppgave.versjon}")),
                    method = eq(HttpMethod.PATCH),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            }
        }
    }
}
