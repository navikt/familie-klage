package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.enhet.BarnetrygdEnhet
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.klage.infrastruktur.exception.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import org.springframework.http.HttpMethod as SpringHttpMethod

class OppgaveClientTest {
    private val baseUrl = URI("http://localhost:8080")
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val oppgaveClient =
        OppgaveClient(
            restClient = restClientBuilder.build(),
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

            mockServer
                .expect(requestTo("$baseUrl/api/oppgave/$oppgaveId"))
                .andExpect(method(SpringHttpMethod.GET))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.success(eksisterendeOppgave)),
                        MediaType.APPLICATION_JSON,
                    ),
                )
            mockServer
                .expect(
                    requestTo("$baseUrl/api/oppgave/$oppgaveId/enhet/${nyEnhet.enhetsnummer}?fjernMappeFraOppgave=true&nullstillTilordnetRessurs=true&versjon=${eksisterendeOppgave.versjon}"),
                ).andExpect(method(SpringHttpMethod.PATCH))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(oppgaveId))),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            // Act
            val oppgaveResponse =
                oppgaveClient.patchEnhetPåOppgave(
                    oppgaveId = oppgaveId,
                    nyEnhet = nyEnhet,
                    fjernMappeFraOppgave = true,
                )

            // Assert
            mockServer.verify()
            assertThat(oppgaveResponse.oppgaveId).isEqualTo(oppgaveId)
        }

        @Test
        fun `skal patche enhet på oppgave uten å tilbakestille tilordnet ressurs eller mappe`() {
            // Arrange
            val oppgaveId = 1L
            val nyEnhet = BarnetrygdEnhet.OSLO
            val eksisterendeOppgave = Oppgave(id = oppgaveId, versjon = 1)

            mockServer
                .expect(requestTo("$baseUrl/api/oppgave/$oppgaveId"))
                .andExpect(method(SpringHttpMethod.GET))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.success(eksisterendeOppgave)),
                        MediaType.APPLICATION_JSON,
                    ),
                )
            mockServer
                .expect(
                    requestTo("$baseUrl/api/oppgave/$oppgaveId/enhet/${nyEnhet.enhetsnummer}?fjernMappeFraOppgave=false&nullstillTilordnetRessurs=false&versjon=${eksisterendeOppgave.versjon}"),
                ).andExpect(method(SpringHttpMethod.PATCH))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(oppgaveId))),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            // Act
            val oppgaveResponse =
                oppgaveClient.patchEnhetPåOppgave(
                    oppgaveId = oppgaveId,
                    nyEnhet = nyEnhet,
                    fjernMappeFraOppgave = false,
                    nullstillTilordnetRessurs = false,
                )

            // Assert
            mockServer.verify()
            assertThat(oppgaveResponse.oppgaveId).isEqualTo(oppgaveId)
        }

        @Test
        fun `skal håndtere feil ved patching an enhet på oppgave`() {
            // Arrange
            val oppgaveId = 1L
            val nyEnhet = BarnetrygdEnhet.OSLO
            val eksisterendeOppgave = Oppgave(id = oppgaveId, versjon = 1)

            mockServer
                .expect(requestTo("$baseUrl/api/oppgave/$oppgaveId"))
                .andExpect(method(SpringHttpMethod.GET))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.success(eksisterendeOppgave)),
                        MediaType.APPLICATION_JSON,
                    ),
                )
            mockServer
                .expect(
                    requestTo("$baseUrl/api/oppgave/$oppgaveId/enhet/${nyEnhet.enhetsnummer}?fjernMappeFraOppgave=false&nullstillTilordnetRessurs=true&versjon=${eksisterendeOppgave.versjon}"),
                ).andExpect(method(SpringHttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST))

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
            mockServer.verify()
        }
    }
}
