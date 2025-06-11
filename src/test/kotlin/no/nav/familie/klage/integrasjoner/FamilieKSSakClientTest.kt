package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.util.UUID

class FamilieKSSakClientTest {
    private val restOperations = mockk<RestOperations>()

    private val familieKSSakClient =
        FamilieKSSakClient(
            restOperations = restOperations,
            familieKsSakUri = URI.create("http://localhost:8080"),
        )

    @Nested
    inner class OpprettRevurdering {
        @Test
        fun `skal opprette revurdering`() {
            // Arrange
            val eksternFagsakId = "1"
            val eksternBehandlingId = UUID.randomUUID()

            val fakeOpprettRevurderingResponse =
                OpprettRevurderingResponse(
                    opprettet = Opprettet(eksternBehandlingId.toString()),
                )

            val uri =
                URI.create(
                    "http://localhost:8080/api/ekstern/fagsak/$eksternFagsakId/klagebehandling/$eksternBehandlingId/opprett-revurdering-klage",
                )

            every {
                restOperations.exchange<Ressurs<OpprettRevurderingResponse>>(
                    url = any<URI>(),
                    method = any<HttpMethod>(),
                    requestEntity = any<HttpEntity<Void>>(),
                )
            } returns
                ResponseEntity.ok(
                    Ressurs.success(
                        fakeOpprettRevurderingResponse,
                    ),
                )

            // Act
            val opprettRevurderingResponse =
                familieKSSakClient.opprettRevurdering(
                    eksternFagsakId = eksternFagsakId,
                    eksternBehandlingId = eksternBehandlingId,
                )

            // Assert
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<OpprettRevurderingResponse>>(
                    eq(uri),
                    eq(HttpMethod.POST),
                    any<HttpEntity<Void>>(),
                )
            }
            assertThat(opprettRevurderingResponse).isEqualTo(fakeOpprettRevurderingResponse)
        }
    }
}
