package no.nav.familie.klage.brev.brevmottaker

import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.brev.dto.BrevmottakereDto
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
import java.util.UUID

class BrevmottakerControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var brevRepository: BrevRepository

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

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
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
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = "ukjent"))

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
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
        fun `skal hente brevmottakere`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(
                    brevmottakerPersonMedIdent,
                    brevmottakerPersonUtenIdent,
                ),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)
            brevRepository.insert(brev)

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.melding).isEqualTo("Innhenting av data var vellykket")
            assertThat(exchange.body?.frontendFeilmelding).isNull()
            assertThat(exchange.body?.data?.organisasjoner).isEmpty()
            assertThat(exchange.body?.data?.personer).hasSize(2)
            assertThat(exchange.body?.data?.personer?.filterIsInstance<BrevmottakerPersonMedIdent>()).anySatisfy {
                assertThat(it.personIdent).isEqualTo(brevmottakerPersonMedIdent.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
            }
            assertThat(exchange.body?.data?.personer?.filterIsInstance<BrevmottakerPersonUtenIdent>()).anySatisfy {
                assertThat(it.id).isEqualTo(brevmottakerPersonUtenIdent.id)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(brevmottakerPersonUtenIdent.landkode)
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

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto()

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerPersonUtenIdentDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo("Saksbehandler julenissen har ikke tilgang til behandling=${behandling.id}")
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo(
                "Mangler tilgang til opplysningene. Årsak: Mock sier: Du har ikke tilgang til person ikkeTilgang",
            )
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal returnere 403 forbidden når token ikke har påkrevd role`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.veileder))

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto()

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerPersonUtenIdentDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo(
                "Saksbehandler julenissen har ikke tilgang til å utføre denne operasjonen som krever minimumsrolle SAKSBEHANDLER",
            )
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo("Mangler nødvendig saksbehandlerrolle for å utføre handlingen")
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal retunere 400 Bad Request når dtoen ikke er gyldig`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "Fornavn mellomnavn Etternavn",
                adresselinje1 = "Marsveien 1, X771, Mars",
                postnummer = "10",
                poststed = "Mars",
                landkode = "NO",
            )

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerPersonUtenIdentDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
            assertThat(exchange.body?.melding).isEqualTo("Postnummer må være 4 siffer.")
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo("Postnummer må være 4 siffer.")
            assertThat(exchange.body?.data).isNull()
        }

        @Test
        fun `skal opprette brevmottaker`() {
            // Arrange
            val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)
            brevRepository.insert(brev)

            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerPersonUtenIdentDto(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "Fornavn mellomnavn Etternavn",
                adresselinje1 = "Marsveien 1, X771, Mars",
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity<NyBrevmottakerPersonUtenIdentDto>(nyBrevmottakerDto, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(exchange.body?.melding).isEqualTo("Innhenting av data var vellykket")
            assertThat(exchange.body?.frontendFeilmelding).isNull()
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(exchange.body?.data?.organisasjoner).isEmpty()
            assertThat(exchange.body?.data?.personer).hasSize(2)
            assertThat(exchange.body?.data?.personer?.filterIsInstance<BrevmottakerPersonMedIdent>()).anySatisfy {
                assertThat(it.personIdent).isEqualTo(brevmottakerPersonMedIdent.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
            }
            assertThat(exchange.body?.data?.personer?.filterIsInstance<BrevmottakerPersonUtenIdent>()).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerDto.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerDto.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerDto.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerDto.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerDto.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerDto.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerDto.landkode)
            }
        }
    }

    @Nested
    inner class SlettBrevmottakereTest {
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
            headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}/${UUID.randomUUID()}"),
                HttpMethod.DELETE,
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
            val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
            headers.setBearerAuth(onBehalfOfToken(role = "ukjent"))

            // Act
            val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
                localhost("$baseUrl/${behandling.id}/${UUID.randomUUID()}"),
                HttpMethod.DELETE,
                HttpEntity<Void>(null, headers),
            )

            // Assert
            assertThat(exchange.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
            assertThat(exchange.body?.melding).isEqualTo("Bruker har ikke tilgang til saksbehandlingsløsningen")
            assertThat(exchange.body?.frontendFeilmelding).isEqualTo("Du mangler tilgang til denne saksbehandlingsløsningen")
            assertThat(exchange.body?.data).isNull()
        }
    }

    @Test
    fun `skal slette brevmottaker`() {
        // Arrange
        val fagsak = testoppsettService.lagreFagsak(DomainUtil.fagsak(stønadstype = Stønadstype.BARNETRYGD))
        val behandling = testoppsettService.lagreBehandling(behandling(fagsak))
        headers.setBearerAuth(onBehalfOfToken(role = rolleConfig.ba.saksbehandler))

        val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
        val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent()
        val brevmottakere = DomainUtil.lagBrevmottakere(
            personer = listOf(
                brevmottakerPersonMedIdent,
                brevmottakerPersonUtenIdent,
            ),
        )
        val brev = DomainUtil.lagBrev(mottakere = brevmottakere)
        brevRepository.insert(brev)

        // Act
        val exchange = restTemplate.exchange<Ressurs<BrevmottakereDto>>(
            localhost("$baseUrl/${behandling.id}/${brevmottakerPersonUtenIdent.id}"),
            HttpMethod.DELETE,
            HttpEntity<Void>(null, headers),
        )

        // Assert
        assertThat(exchange.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(exchange.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(exchange.body?.melding).isEqualTo("Innhenting av data var vellykket")
        assertThat(exchange.body?.frontendFeilmelding).isNull()
        assertThat(exchange.body?.data?.organisasjoner).isEmpty()
        assertThat(exchange.body?.data?.personer).hasSize(1)
        assertThat(exchange.body?.data?.personer?.filterIsInstance<BrevmottakerPersonMedIdent>()).anySatisfy {
            assertThat(it.personIdent).isEqualTo(brevmottakerPersonMedIdent.personIdent)
            assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
            assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
        }
    }
}
