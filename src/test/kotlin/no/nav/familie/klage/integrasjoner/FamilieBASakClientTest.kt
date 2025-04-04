package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.net.URI
import java.util.UUID
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
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

class FamilieBASakClientTest {
    private val restOperations = mockk<RestOperations>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val familieBASakClient = FamilieBASakClient(
        restOperations = restOperations,
        familieBaSakUri = URI.create("http://localhost:8080"),
        featureToggleService = featureToggleService,
    )

    @Nested
    inner class OpprettRevurdering {
        @Test
        fun `skal opprette revurdering uten å sende behandlingId`() {
            // Arrange
            val eksternFagsakId = "1"
            val klagebehandlingId = UUID.randomUUID()

            val fakeOpprettRevurderingResponse = OpprettRevurderingResponse(
                opprettet = Opprettet(klagebehandlingId.toString())
            )

            val uri = URI.create(
                "http://localhost:8080/api/ekstern/fagsaker/$eksternFagsakId/opprett-revurdering-klage"
            )

            every {
                restOperations.exchange<Ressurs<OpprettRevurderingResponse>>(
                    url = any<URI>(),
                    method = any<HttpMethod>(),
                    requestEntity = any<HttpEntity<Void>>()
                )
            } returns ResponseEntity.ok(
                Ressurs.success(
                    fakeOpprettRevurderingResponse
                )
            )

            every { featureToggleService.isEnabled(Toggle.SEND_BEHANDLING_ID_VED_OPPRETTING_AV_REVURDERING_KLAGE) } returns false

            // Act
            val opprettRevurderingResponse = familieBASakClient.opprettRevurdering(
                eksternFagsakId = eksternFagsakId,
                klagebehandlingId = klagebehandlingId
            )

            // Assert
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<OpprettRevurderingResponse>>(
                    eq(uri),
                    eq(HttpMethod.POST),
                    any<HttpEntity<Void>>()
                )
            }
            assertThat(opprettRevurderingResponse).isEqualTo(fakeOpprettRevurderingResponse)
        }

        @Test
        fun `skal opprette revurdering`() {
            // Arrange
            val eksternFagsakId = "1"
            val klagebehandlingId = UUID.randomUUID()

            val fakeOpprettRevurderingResponse = OpprettRevurderingResponse(
                opprettet = Opprettet(klagebehandlingId.toString())
            )

            val uri = URI.create(
                "http://localhost:8080/api/ekstern/fagsak/$eksternFagsakId/klagebehandling/$klagebehandlingId/opprett-revurdering-klage"
            )

            every {
                restOperations.exchange<Ressurs<OpprettRevurderingResponse>>(
                    url = any<URI>(),
                    method = any<HttpMethod>(),
                    requestEntity = any<HttpEntity<Void>>()
                )
            } returns ResponseEntity.ok(
                Ressurs.success(
                    fakeOpprettRevurderingResponse
                )
            )

            every { featureToggleService.isEnabled(Toggle.SEND_BEHANDLING_ID_VED_OPPRETTING_AV_REVURDERING_KLAGE) } returns true

            // Act
            val opprettRevurderingResponse = familieBASakClient.opprettRevurdering(
                eksternFagsakId = eksternFagsakId,
                klagebehandlingId = klagebehandlingId
            )

            // Assert
            verify(exactly = 1) {
                restOperations.exchange<Ressurs<OpprettRevurderingResponse>>(
                    eq(uri),
                    eq(HttpMethod.POST),
                    any<HttpEntity<Void>>()
                )
            }
            assertThat(opprettRevurderingResponse).isEqualTo(fakeOpprettRevurderingResponse)
        }
    }
}