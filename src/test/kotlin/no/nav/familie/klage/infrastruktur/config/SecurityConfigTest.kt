package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status.FEILET
import no.nav.familie.kontrakter.felles.Ressurs.Status.IKKE_TILGANG
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED

class SecurityConfigTest(
    @param:Autowired private val rolleConfig: RolleConfig,
) : OppslagSpringRunnerTest() {
    private lateinit var behandlingUrl: String

    @BeforeEach
    fun setUp() {
        val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
        val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
        behandlingUrl = localhost("/api/behandling/${behandling.id}")
    }

    @Nested
    inner class PermitAll {
        @Test
        fun `ping er tilgjengelig uten autentisering`() {
            val response =
                restTemplate.exchange<String>(
                    url = localhost("/api/ping"),
                    method = GET,
                    requestEntity = HttpEntity(null, headers),
                )

            assertThat(response.statusCode).isEqualTo(OK)
            assertThat(response.body).isEqualTo("pong")
        }
    }

    @Nested
    inner class UtenToken {
        @Test
        fun `api-kall uten token returnerer 401`() {
            val response =
                restTemplate.exchange<Ressurs<Any>>(
                    url = behandlingUrl,
                    method = GET,
                    requestEntity = HttpEntity(null, headers),
                )
            assertThat(response.statusCode).isEqualTo(UNAUTHORIZED)
            assertThat(response.body?.status).isEqualTo(FEILET)
            assertThat(response.body?.frontendFeilmelding).isEqualTo("En uventet feil oppstod: Kall ikke autorisert")
        }
    }

    @Nested
    inner class UkjentRolle {
        @Test
        fun `api-kall med ukjent rolle returnerer 403 med IKKE_TILGANG`() {
            headers.setBearerAuth(onBehalfOfToken(role = "ukjent"))

            val response =
                restTemplate.exchange<Ressurs<Any>>(
                    url = behandlingUrl,
                    method = GET,
                    requestEntity = HttpEntity(null, headers),
                )

            assertThat(response.statusCode).isEqualTo(FORBIDDEN)
            assertThat(response.body?.status).isEqualTo(IKKE_TILGANG)
            assertThat(response.body?.melding).isEqualTo("Bruker har ikke tilgang til saksbehandlingsløsningen")
            assertThat(response.body?.frontendFeilmelding).isEqualTo("Du mangler tilgang til denne saksbehandlingsløsningen")
        }
    }

    @Nested
    inner class VeilederRolle {
        @BeforeEach
        fun setUpVeilederToken() {
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))
        }

        @Test
        fun `veileder har tilgang til GET-endepunkt`() {
            val response =
                restTemplate.exchange<Ressurs<Any>>(
                    url = behandlingUrl,
                    method = GET,
                    requestEntity = HttpEntity(null, headers),
                )

            assertThat(response.statusCode).isNotEqualTo(FORBIDDEN)
            assertThat(response.statusCode).isNotEqualTo(UNAUTHORIZED)
        }

        @Test
        fun `veileder får 403 på POST-endepunkt som krever SAKSBEHANDLER`() {
            val response =
                restTemplate.exchange<Ressurs<Any>>(
                    url = "$behandlingUrl/ferdigstill",
                    method = POST,
                    requestEntity = HttpEntity(null, headers),
                )

            assertThat(response.statusCode).isEqualTo(FORBIDDEN)
            assertThat(response.body?.status).isEqualTo(IKKE_TILGANG)
        }
    }

    @Nested
    inner class SaksbehandlerRolle {
        @BeforeEach
        fun setUpSaksbehandlerToken() {
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))
        }

        @Test
        fun `saksbehandler har tilgang til GET-endepunkt`() {
            val response =
                restTemplate.exchange<Ressurs<Any>>(
                    url = behandlingUrl,
                    method = GET,
                    requestEntity = HttpEntity(null, headers),
                )

            assertThat(response.statusCode).isNotEqualTo(FORBIDDEN)
            assertThat(response.statusCode).isNotEqualTo(UNAUTHORIZED)
        }

        @Test
        fun `saksbehandler har tilgang til POST-endepunkt som krever SAKSBEHANDLER`() {
            val response =
                restTemplate.exchange<Ressurs<Any>>(
                    url = "$behandlingUrl/ferdigstill",
                    method = POST,
                    requestEntity = HttpEntity(null, headers),
                )

            assertThat(response.statusCode).isNotEqualTo(FORBIDDEN)
            assertThat(response.statusCode).isNotEqualTo(UNAUTHORIZED)
        }
    }
}
