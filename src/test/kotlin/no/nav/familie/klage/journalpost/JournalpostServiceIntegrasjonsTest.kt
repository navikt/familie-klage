package no.nav.familie.klage.journalpost

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.klage.infrastruktur.exception.ManglerTilgang
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI

class JournalpostServiceIntegrasjonsTest {
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var wiremockServerItem: WireMockServer
    private lateinit var familieIntegrasjonerClient: FamilieIntegrasjonerClient
    private lateinit var journalpostService: JournalpostService


    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        val integrasjonUri = URI.create(wiremockServerItem.baseUrl())
        familieIntegrasjonerClient = FamilieIntegrasjonerClient(
            restOperations = restOperations,
            integrasjonUri = integrasjonUri,
            integrasjonerConfig = IntegrasjonerConfig(integrasjonUri)
        )
        journalpostService = JournalpostService(familieIntegrasjonerClient)
    }

    @Test
    fun `finnJournalposter() håndterer forbidden på riktig måte`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/api/journalpost"))
                .willReturn(
                    WireMock.forbidden().withBody(
                        """
                    {
                      "data": null,
                      "status": "IKKE_TILGANG",
                      "melding": "Bruker eller system har ikke tilgang til saf ressurs",
                      "frontendFeilmelding": null,
                      "stacktrace": null,
                      "callId": null
                    }
                """.trimIndent()
                    )
                )
        )

        // Act
        val manglerTilgang = assertThrows<ManglerTilgang> {
            journalpostService.finnJournalposter(
                personIdent = "12345678901",
                stønadType = Stønadstype.BARNETRYGD,
            )
        }

        // Assert
        Assertions.assertThat(manglerTilgang.melding).isEqualTo("Bruker mangler tilgang til etterspurt oppgave")
    }

}