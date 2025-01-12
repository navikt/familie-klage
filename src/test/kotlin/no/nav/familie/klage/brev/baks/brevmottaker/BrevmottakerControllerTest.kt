package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DtoTestUtil
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

    @Nested
    inner class OpprettBrevmottakereTest {
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

            val brevmottaker = DomainUtil.lagBrevmottaker(
                behandlingId = behandling.id,
                mottakertype = Mottakertype.FULLMEKTIG,
            )

            brevmottakerRepository.insert(brevmottaker)

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto()

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerDto>(nyBrevmottakerDto, headers),
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
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            val brevmottaker = DomainUtil.lagBrevmottaker(
                behandlingId = behandling.id,
                mottakertype = Mottakertype.FULLMEKTIG,
            )

            brevmottakerRepository.insert(brevmottaker)

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto()

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal retunere 400 Bad Request når dtoen ikke er gyldig`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            val brevmottaker = DomainUtil.lagBrevmottaker(
                behandlingId = behandling.id,
                mottakertype = Mottakertype.FULLMEKTIG,
            )

            brevmottakerRepository.insert(brevmottaker)

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "Fornavn mellomnavn Etternavn",
                adresselinje1 = "Marsveien 1, X771, Mars",
                postnummer = "10",
                poststed = "Mars",
                landkode = "NO",
            )

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal opprette brevmottaker`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            val brevmottaker = DomainUtil.lagBrevmottaker(
                behandlingId = behandling.id,
                mottakertype = Mottakertype.FULLMEKTIG,
            )

            brevmottakerRepository.insert(brevmottaker)

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "Fornavn mellomnavn Etternavn",
                adresselinje1 = "Marsveien 1, X771, Mars",
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            // Act
            val exchange = restTemplate.exchange<Ressurs<List<BrevmottakerDto>>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.data).hasSize(2)
            assertThat(exchange.body?.data).anySatisfy {
                assertThat(it.id).isEqualTo(brevmottaker.id)
                assertThat(it.mottakertype).isEqualTo(brevmottaker.mottakertype)
                assertThat(it.navn).isEqualTo(brevmottaker.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottaker.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottaker.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottaker.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottaker.poststed)
                assertThat(it.landkode).isEqualTo(brevmottaker.landkode)
            }
            assertThat(exchange.body?.data).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakertype).isEqualTo(nyBrevmottakerDto.mottakertype)
                assertThat(it.navn).isEqualTo(nyBrevmottakerDto.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerDto.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerDto.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerDto.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerDto.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerDto.landkode)
            }
        }
    }
}
