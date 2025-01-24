package no.nav.familie.klage.brev.brevmottaker.baks

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BaksBrevmottakerOppretterTest {
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val brevService: BrevService = mockk()
    private val brevRepository: BrevRepository = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()

    private val baksBrevmottakerOppretter: BaksBrevmottakerOppretter = BaksBrevmottakerOppretter(
        behandlingService = behandlingService,
        fagsakService = fagsakService,
        brevService = brevService,
        brevRepository = brevRepository,
        personopplysningerService = personopplysningerService,
    )

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class OpprettBrevmottakerTest {
        @Test
        fun `skal kaste exception om behandling ikke er redigerbar`() {
            // Arrange
            val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om det allerede finnes en brevmottaker med samme MottakerRolle`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.FULLMAKT,
                navn = "navn",
                adresselinje1 = "adresse1",
                adresselinje2 = "adresse2",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.FULLMAKT,
            )
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Kan ikke ha duplikate mottakertyper. FULLMAKT finnes allerede.")
        }

        @Test
        fun `skal kaste exception om bruker med utenlandsk adresse ikke har samme navn som i personopplysningene`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = "et annet navn")

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
        }

        @Test
        fun `skal kaste exception om dødsbo ikke har samme navn som i personopplysningene`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.DØDSBO,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = "et annet navn")

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til dødsbo når det allerede finnes en brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.DØDSBO,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Kan ikke legge til dødsbo når det allerede finnes brevmottakere.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til en brevmottaker når det allerede finnes en dødsbo brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.DØDSBO,
            )
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til en brevmottaker som ikke er VERGE eller FULLMEKTIG når det allerede finnes en brevmottaker med utenlandsk adresse`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
            )
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til en brevmottaker som ikke har mottakertype bruker med utenlandsk adresse om det allerede finnes en brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.FULLMAKT,
            )
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception = assertThrows<Feil> {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
            }
            assertThat(exception.message).isEqualTo(
                "Kan kun legge til bruker med utenlandsk adresse om det finnes en brevmottaker allerede.",
            )
        }

        @EnumSource(value = MottakerRolle::class)
        @ParameterizedTest
        fun `skal opprette brevmottaker når det allerede finnes brevmottakere`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(identer = setOf(PersonIdent("01010199999")))

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottakerPersonUtenIdent = baksBrevmottakerOppretter.opprettBrevmottaker(
                behandling.id,
                nyBrevmottakerPersonUtenIdent,
            )

            // Act & assert
            assertThat(brevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(brevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(brevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(brevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(brevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(brevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(brevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(brevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }

        @EnumSource(value = MottakerRolle::class)
        @ParameterizedTest
        fun `skal opprette brevmottaker når brevmottakere i brev er null`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brev = DomainUtil.lagBrev(mottakere = null)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(identer = setOf(PersonIdent("01010199999")))

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottakerPersonUtenIdent = baksBrevmottakerOppretter.opprettBrevmottaker(
                behandling.id,
                nyBrevmottakerPersonUtenIdent,
            )

            // Act & assert
            assertThat(brevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(brevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(brevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(brevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(brevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(brevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(brevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(brevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }

        @EnumSource(value = MottakerRolle::class)
        @ParameterizedTest
        fun `skal opprette brevmottaker når brevmottakere i brev er tom`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brev = DomainUtil.lagBrev(mottakere = DomainUtil.lagBrevmottakere(personer = emptyList()))

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(identer = setOf(PersonIdent("01010199999")))

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottakerPersonUtenIdent = baksBrevmottakerOppretter.opprettBrevmottaker(
                behandling.id,
                nyBrevmottakerPersonUtenIdent,
            )

            // Act & assert
            assertThat(brevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(brevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(brevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(brevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(brevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(brevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(brevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(brevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        @ParameterizedTest
        fun `skal opprette brevmottaker selv om personopplysningsnavnet er forskjellig for mottakertyper som ikke skal være preutfylt`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = "ikke samme navn")

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(identer = setOf(PersonIdent("01010199999")))

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottakerPersonUtenIdent = baksBrevmottakerOppretter.opprettBrevmottaker(
                behandling.id,
                nyBrevmottakerPersonUtenIdent,
            )

            // Act & assert
            assertThat(brevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(brevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(brevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(brevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(brevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(brevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(brevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(brevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["VERGE", "FULLMAKT"],
            mode = EnumSource.Mode.INCLUDE,
        )
        @ParameterizedTest
        fun `skal opprette brevmottaker som kan kombineres med en allerede eksisterende brevmottaker med MottakeRrolle bruker med utenlandsk adresse`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
            )
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(identer = setOf(PersonIdent("01010199999")))

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val opprettetBrevmottakerPersonUtenIdent = baksBrevmottakerOppretter.opprettBrevmottaker(
                behandling.id,
                nyBrevmottakerPersonUtenIdent,
            )

            // Act & assert
            assertThat(opprettetBrevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(opprettetBrevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(opprettetBrevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(opprettetBrevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(opprettetBrevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(opprettetBrevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(opprettetBrevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(opprettetBrevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["VERGE", "FULLMAKT"],
            mode = EnumSource.Mode.INCLUDE,
        )
        @ParameterizedTest
        fun `skal opprette brevmottaker med mottakertype bruker med utenlandsk adresse samtidig som det allerede eksisterede brevmottakere som lar seg kombinere med brukere med utenlandsk adresse`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling()

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "navn",
                adresselinje1 = "Adresse 1, Mars, 1337",
                adresselinje2 = null,
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(
                mottakerRolle = mottakerRolle,
            )
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
            )
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottakerPersonUtenIdent.navn)

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(identer = setOf(PersonIdent("01010199999")))

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val opprettetBrevmottakerPersonUtenIdent = baksBrevmottakerOppretter.opprettBrevmottaker(
                behandling.id,
                nyBrevmottakerPersonUtenIdent,
            )

            // Act & assert
            assertThat(opprettetBrevmottakerPersonUtenIdent.id).isNotNull()
            assertThat(opprettetBrevmottakerPersonUtenIdent.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
            assertThat(opprettetBrevmottakerPersonUtenIdent.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
            assertThat(opprettetBrevmottakerPersonUtenIdent.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
            assertThat(opprettetBrevmottakerPersonUtenIdent.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
            assertThat(opprettetBrevmottakerPersonUtenIdent.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
            assertThat(opprettetBrevmottakerPersonUtenIdent.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
            assertThat(opprettetBrevmottakerPersonUtenIdent.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
        }
    }
}
