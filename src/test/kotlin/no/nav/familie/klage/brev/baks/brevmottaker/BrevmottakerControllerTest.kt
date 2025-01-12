package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
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

class BrevmottakerControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var brevmottakerRepository: BrevmottakerRepository

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    private val baseUrl = "/api/brevmottaker"

    @Nested
    inner class HentBrevmottakereTest {
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
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            val brevmottaker1 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)
            val brevmottaker2 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)

            brevmottakerRepository.insertAll(listOf(brevmottaker1, brevmottaker2))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal returnere 403 forbidden når token ikke har påkrevd role`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = "ukjent"))

            val brevmottaker1 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)
            val brevmottaker2 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)

            brevmottakerRepository.insertAll(listOf(brevmottaker1, brevmottaker2))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal hente brevmottakere`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            val brevmottaker1 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)
            val brevmottaker2 = DomainUtil.lagBrevmottaker(behandlingId = behandling.id)

            brevmottakerRepository.insertAll(listOf(brevmottaker1, brevmottaker2))

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.data).hasSize(2)
            assertThat(exchange.body?.data).anySatisfy {
                assertThat(it.id).isEqualTo(brevmottaker1.id)
                assertThat(it.mottakertype).isEqualTo(brevmottaker1.mottakertype)
                assertThat(it.navn).isEqualTo(brevmottaker1.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottaker1.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottaker1.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottaker1.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottaker1.poststed)
                assertThat(it.landkode).isEqualTo(brevmottaker1.landkode)
            }
            assertThat(exchange.body?.data).anySatisfy {
                assertThat(it.id).isEqualTo(brevmottaker2.id)
                assertThat(it.mottakertype).isEqualTo(brevmottaker2.mottakertype)
                assertThat(it.navn).isEqualTo(brevmottaker2.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottaker2.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottaker2.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottaker2.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottaker2.poststed)
                assertThat(it.landkode).isEqualTo(brevmottaker2.landkode)
            }
        }
    }
}
