package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.config.PdlConfig
import no.nav.familie.kontrakter.felles.Tema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI

class PdlClientTest {
    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val pdlClient = PdlClient(PdlConfig(URI.create(BASE_URL)), restClientBuilder.build())

    @ParameterizedTest
    @EnumSource(value = Tema::class, names = ["BAR", "ENF", "KON"])
    fun `skal slå opp relasjonene med tema og behandlingsnummer for gitt tema`(tema: Tema) {
        // Arrange
        mockServer
            .expect(requestTo("$BASE_URL/graphql"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Tema", tema.name))
            .andExpect(header("behandlingsnummer", tema.behandlingsnummer))
            .andRespond(withSuccess(SVAR_UTEN_RELASJONER, MediaType.APPLICATION_JSON))

        // Act
        val personMedRelasjoner = pdlClient.hentPersonMedRelasjoner(PERSONIDENT, tema)

        // Assert
        mockServer.verify()
        assertThat(personMedRelasjoner).isEqualTo(PdlPersonMedRelasjoner(forelderBarnRelasjon = emptyList(), sivilstand = emptyList()))
    }

    companion object {
        private const val BASE_URL = "http://localhost:8080"
        private const val PERSONIDENT = "01010112345"
        private const val SVAR_UTEN_RELASJONER =
            """{"data":{"personBolk":[{"ident":"$PERSONIDENT","code":"ok","person":{"forelderBarnRelasjon":[],"sivilstand":[]}}]}}"""
    }
}
