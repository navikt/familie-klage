package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.baks.brevmottaker.BrevmottakerDto
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.formkrav.FormRepository
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class BaksBrevControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var baksBrevRepository: BaksBrevRepository

    @Autowired
    private lateinit var formRepository: FormRepository

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    private val baseUrl = "/api/baks/brev"

    @Nested
    inner class HentBrevTest {
        @Test
        fun `skal returnere 403 forbidden når man ikke har tilgang til personen med relasjoner for behandlingen`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(
                DomainUtil.fagsak(
                    stønadstype = Stønadstype.BARNETRYGD,
                    person = FagsakPerson(
                        identer = setOf(
                            PersonIdent(".*ikkeTilgang.*"),
                        ),
                    ),
                ),
            )
            val behandling = testoppsettService.lagreBehandling(DomainUtil.behandling(fagsak = fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}/pdf"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo(
                "Saksbehandler julenissen har ikke tilgang til behandling=${behandling.id}",
            )
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo(
                "Mangler tilgang til opplysningene. Årsak: Mock sier: Du har ikke tilgang til person ikkeTilgang",
            )
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal returnere 403 forbidden når token ikke har påkrevd role`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(DomainUtil.behandling(fagsak = fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = "ukjent"))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}/pdf"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo("Bruker har ikke tilgang til saksbehandlingsløsningen")
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo("Du mangler tilgang til denne saksbehandlingsløsningen")
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal hente brev`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(DomainUtil.behandling(fagsak = fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            val baksBrev = DomainUtil.lagBaksBrev(behandlingId = behandling.id)

            baksBrevRepository.insert(baksBrev)

            // Act
            val exchange = restTemplate.exchange<Ressurs<ByteArray>>(
                localhost("$baseUrl/${behandling.id}/pdf"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.melding).isEqualTo("Innhenting av data var vellykket")
            assertThat(exchange.body?.frontendFeilmelding).isNull()
            assertThat(exchange.body?.data).isEqualTo(baksBrev.pdfSomBytes())
        }
    }

    @Nested
    inner class OpprettEllerOppdaterBrevTest {
        @Test
        fun `skal returnere 403 forbidden når man ikke har tilgang til personen med relasjoner for behandlingen`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(
                DomainUtil.fagsak(
                    stønadstype = Stønadstype.BARNETRYGD,
                    person = FagsakPerson(
                        identer = setOf(
                            PersonIdent(".*ikkeTilgang.*"),
                        ),
                    ),
                ),
            )
            val behandling = testoppsettService.lagreBehandling(
                DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV),
            )
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo(
                "Saksbehandler julenissen har ikke tilgang til behandling=${behandling.id}",
            )
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo(
                "Mangler tilgang til opplysningene. Årsak: Mock sier: Du har ikke tilgang til person ikkeTilgang",
            )
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal returnere 403 forbidden når token ikke har påkrevd role`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(
                DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD),
            )
            val behandling = testoppsettService.lagreBehandling(
                DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV),
            )
            headers.setBearerAuth(onBehalfOfToken(role = "ukjent"))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo("Bruker har ikke tilgang til saksbehandlingsløsningen")
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo("Du mangler tilgang til denne saksbehandlingsløsningen")
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal oppdatere brev`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(
                DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD),
            )
            val behandling = testoppsettService.lagreBehandling(
                DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV),
            )
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            val baksBrev = DomainUtil.lagBaksBrev(behandlingId = behandling.id)

            baksBrevRepository.insert(baksBrev)

            formRepository.insert(
                Form(
                    behandlingId = behandling.id,
                    klagePart = FormVilkår.IKKE_OPPFYLT,
                    brevtekst = "brevtekts",
                ),
            )

            // Act
            val exchange = restTemplate.exchange<Ressurs<ByteArray>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.melding).isEqualTo("Innhenting av data var vellykket")
            assertThat(exchange.body?.frontendFeilmelding).isNull()
            assertThat(exchange.body?.data).isNotNull()
        }

        @Test
        fun `skal opprette brev`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(
                DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD),
            )
            val behandling = testoppsettService.lagreBehandling(
                DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV),
            )
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            formRepository.insert(
                Form(
                    behandlingId = behandling.id,
                    klagePart = FormVilkår.IKKE_OPPFYLT,
                    brevtekst = "brevtekts",
                ),
            )

            // Act
            val exchange = restTemplate.exchange<Ressurs<ByteArray>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.melding).isEqualTo("Innhenting av data var vellykket")
            assertThat(exchange.body?.frontendFeilmelding).isNull()
            assertThat(exchange.body?.data).isNotNull()
        }
    }
}
