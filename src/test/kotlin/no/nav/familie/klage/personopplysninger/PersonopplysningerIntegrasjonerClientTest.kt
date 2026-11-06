package no.nav.familie.klage.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
import no.nav.familie.klage.integrasjoner.IntegrasjonerClientProvider
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import org.springframework.http.HttpMethod as SpringHttpMethod

class PersonopplysningerIntegrasjonerClientTest {
    private val baseUrl = URI("http://localhost:8080")
    private val baseUrlGcp = URI("http://localhost:8081")
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val restClientGcpBuilder = RestClient.builder()
    private val mockServerGcp = MockRestServiceServer.bindTo(restClientGcpBuilder).build()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val client =
        PersonopplysningerIntegrasjonerClient(
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
    fun `skal sjekke egen ansatt mot familie-integrasjoner i valgt cluster`(brukGcp: Boolean) {
        // Arrange
        every { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) } returns brukGcp
        val forventetServer = if (brukGcp) mockServerGcp else mockServer
        val forventetBaseUrl = if (brukGcp) baseUrlGcp else baseUrl

        forventetServer
            .expect(requestTo("$forventetBaseUrl/api/egenansatt"))
            .andExpect(method(SpringHttpMethod.POST))
            .andRespond(
                withSuccess(
                    jsonMapper.writeValueAsString(Ressurs.success(EgenAnsattResponse(erEgenAnsatt = true))),
                    MediaType.APPLICATION_JSON,
                ),
            )

        // Act
        val erEgenAnsatt = client.egenAnsatt("12345678910")

        // Assert
        assertThat(erEgenAnsatt).isTrue()
        mockServer.verify()
        mockServerGcp.verify()
        verify(exactly = 1) { featureToggleService.isEnabled(Toggle.BRUK_FAMILIE_INTEGRASJONER_I_GCP) }
    }
}
