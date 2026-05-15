package no.nav.familie.klage.integrasjoner

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.UUID
import org.springframework.http.HttpMethod as SpringHttpMethod

class FamilieBASakClientTest {
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val baseUrl = "http://localhost:8080"

    private val familieBASakClient =
        FamilieBASakClient(
            familieBaSakUri = URI.create(baseUrl),
            restClient = restClientBuilder.build(),
        )

    @Nested
    inner class OpprettRevurdering {
        @Test
        fun `skal opprette revurdering`() {
            // Arrange
            val eksternFagsakId = "1"
            val eksternBehandlingId = UUID.randomUUID()
            val fakeOpprettRevurderingResponse =
                OpprettRevurderingResponse(opprettet = Opprettet(eksternBehandlingId.toString()))
            val expectedUri =
                "$baseUrl/api/klage/fagsak/$eksternFagsakId/klagebehandling/$eksternBehandlingId/opprett-revurdering-klage"

            mockServer
                .expect(requestTo(expectedUri))
                .andExpect(method(SpringHttpMethod.POST))
                .andRespond(
                    withSuccess(
                        jsonMapper.writeValueAsString(Ressurs.success(fakeOpprettRevurderingResponse)),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            // Act
            val opprettRevurderingResponse =
                familieBASakClient.opprettRevurdering(
                    eksternFagsakId = eksternFagsakId,
                    eksternBehandlingId = eksternBehandlingId,
                )

            // Assert
            mockServer.verify()
            assertThat(opprettRevurderingResponse).isEqualTo(fakeOpprettRevurderingResponse)
        }
    }
}
