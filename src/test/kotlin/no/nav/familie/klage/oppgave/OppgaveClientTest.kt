package no.nav.familie.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.enhet.BarnetrygdEnhet
import no.nav.familie.klage.infrastruktur.exception.IntegrasjonException
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.integrasjoner.IntegrasjonerClientProvider
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpMethod as SpringHttpMethod

class OppgaveClientTest {
    private val baseUrl = URI("http://localhost:8080")
    private val baseUrlGcp = URI("http://localhost:8081")
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val restClientGcpBuilder = RestClient.builder()
    private val mockServerGcp = MockRestServiceServer.bindTo(restClientGcpBuilder).build()
    private val featureToggleService =
        mockk<FeatureToggleService> {
            every { isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns false
        }
    private val oppgaveClient =
        OppgaveClient(
            IntegrasjonerClientProvider(
                restClientFss = restClientBuilder.build(),
                integrasjonFssUri = baseUrl,
                restClientGcp = restClientGcpBuilder.build(),
                integrasjonGcpUri = baseUrlGcp,
                featureToggleService = featureToggleService,
            ),
        )

    @ParameterizedTest(name = "brukGcp={0}")
    @ValueSource(booleans = [false, true])
    fun `skal sende alle kall til familie-integrasjoner i valgt cluster`(brukGcp: Boolean) {
        // Arrange
        every { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns brukGcp
        val forventetServer = if (brukGcp) mockServerGcp else mockServer
        val forventetBaseUrl = if (brukGcp) baseUrlGcp else baseUrl
        val oppgaveId = 1L
        val navIdent = "Z999999"
        val oppgave = Oppgave(id = oppgaveId, versjon = 1)
        val oppgaveResponse = jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(oppgaveId)))

        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/oppgave/opprett"))
            .andExpect(method(SpringHttpMethod.POST))
            .andRespond(withSuccess(oppgaveResponse, MediaType.APPLICATION_JSON))
        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/oppgave/$oppgaveId"))
            .andExpect(method(SpringHttpMethod.GET))
            .andRespond(withSuccess(jsonMapper.writeValueAsString(Ressurs.success(oppgave)), MediaType.APPLICATION_JSON))
        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/oppgave/$oppgaveId/ferdigstill"))
            .andExpect(method(SpringHttpMethod.PATCH))
            .andRespond(withSuccess(oppgaveResponse, MediaType.APPLICATION_JSON))
        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/oppgave/$oppgaveId/oppdater"))
            .andExpect(method(SpringHttpMethod.PATCH))
            .andRespond(withSuccess(oppgaveResponse, MediaType.APPLICATION_JSON))
        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/oppgave/$oppgaveId/fordel?saksbehandler=$navIdent&versjon=1"))
            .andExpect(method(SpringHttpMethod.POST))
            .andRespond(withSuccess(oppgaveResponse, MediaType.APPLICATION_JSON))
        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/oppgave/mappe/sok?enhetsnr=4489&limit=1000"))
            .andExpect(method(SpringHttpMethod.GET))
            .andRespond(
                withSuccess(
                    jsonMapper.writeValueAsString(Ressurs.success(FinnMappeResponseDto(antallTreffTotalt = 0, mapper = emptyList()))),
                    MediaType.APPLICATION_JSON,
                ),
            )
        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/saksbehandler/$navIdent"))
            .andExpect(method(SpringHttpMethod.GET))
            .andRespond(
                withSuccess(
                    jsonMapper.writeValueAsString(
                        Ressurs.success(
                            Saksbehandler(
                                azureId = UUID.randomUUID(),
                                navIdent = navIdent,
                                fornavn = "Fornavn",
                                etternavn = "Etternavn",
                                enhet = "4489",
                                enhetsnavn = "Enhet",
                            ),
                        ),
                    ),
                    MediaType.APPLICATION_JSON,
                ),
            )

        // Act
        oppgaveClient.opprettOppgave(
            OpprettOppgaveRequest(
                ident = null,
                saksId = "1",
                tema = Tema.BAR,
                oppgavetype = Oppgavetype.BehandleSak,
                fristFerdigstillelse = LocalDate.now(),
                beskrivelse = "beskrivelse",
                enhetsnummer = "4489",
                behandlingstema = null,
            ),
        )
        oppgaveClient.finnOppgaveMedId(oppgaveId)
        oppgaveClient.ferdigstillOppgave(oppgaveId)
        oppgaveClient.oppdaterOppgave(oppgave)
        oppgaveClient.fordelOppgave(oppgaveId = oppgaveId, saksbehandler = navIdent, versjon = 1)
        oppgaveClient.finnMapper(enhetnummer = "4489", limit = 1000)
        oppgaveClient.hentSaksbehandlerInfo(navIdent)

        // Assert
        mockServer.verify()
        mockServerGcp.verify()
        verify(exactly = 7) { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) }
    }

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
