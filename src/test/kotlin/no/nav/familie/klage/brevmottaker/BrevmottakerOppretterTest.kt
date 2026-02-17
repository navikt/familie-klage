package no.nav.familie.klage.brevmottaker

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.BRUKER
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.DØDSBO
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.FULLMAKT
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.INSTITUSJON
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.VERGE
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle
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
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE

class BrevmottakerOppretterTest {
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val brevService: BrevService = mockk()
    private val brevRepository: BrevRepository = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val brevmottakerOppretter: BrevmottakerOppretter =
        BrevmottakerOppretter(
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            brevService = brevService,
            brevRepository = brevRepository,
            personopplysningerService = personopplysningerService,
            featureToggleService = featureToggleService,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        every { featureToggleService.isEnabled(Toggle.MANUELL_BREVMOTTAKER_ORGANISASJON) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class OpprettBrevmottakerPersonMedIdent {
        @Test
        fun `skal kaste exception om man prøver å opprette for NyBrevmottakerPersonMedIdent`() {
            // Arrange
            val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            val nyBrevmottakerPersonMedIdent = DomainUtil.lagNyBrevmottakerPersonMedIdent()

            // Act & assert
            val exception =
                assertThrows<UnsupportedOperationException> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonMedIdent)
                }
            assertThat(exception.message).isEqualTo("NyBrevmottakerPersonMedIdent er ikke støttet.")
        }
    }

    @Nested
    inner class OpprettBrevmottakerPersonUtenIdent {
        @Test
        fun `skal kaste exception om behandling ikke er redigerbar`() {
            // Arrange
            val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om behandling ikke er i brev steg`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.OPPRETTET)

            val nyBrevmottakerPersonUtenIdent = DomainUtil.lagNyBrevmottakerPersonUtenIdent()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er i steg ${StegType.OPPRETTET}, forventet steg ${StegType.BREV}.")
        }

        @Test
        fun `skal kaste exception om det allerede finnes en brevmottaker med samme MottakerRolle`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = FULLMAKT,
                    navn = "navn",
                    adresselinje1 = "adresse1",
                    adresselinje2 = "adresse2",
                    postnummer = "0010",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(
                    mottakerRolle = FULLMAKT,
                )
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(exception.message).isEqualTo("Kan ikke ha duplikate MottakerRolle. FULLMAKT finnes allerede for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om bruker med utenlandsk adresse ikke har samme navn som i personopplysningene`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER_MED_UTENLANDSK_ADRESSE,
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(
                exception.message,
            ).isEqualTo("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om dødsbo ikke har samme navn som i personopplysningene`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = DØDSBO,
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(exception.message).isEqualTo("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til dødsbo når det allerede finnes en brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = DØDSBO,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent()
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(exception.message).isEqualTo("Kan ikke legge til dødsbo når det allerede finnes brevmottakere for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til en brevmottaker når det allerede finnes en dødsbo brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER_MED_UTENLANDSK_ADRESSE,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(
                    mottakerRolle = DØDSBO,
                )
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(
                exception.message,
            ).isEqualTo("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til en brevmottaker som ikke er VERGE eller FULLMAKT når det allerede finnes en brevmottaker med utenlandsk adresse`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER_MED_UTENLANDSK_ADRESSE,
                )
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(
                exception.message,
            ).isEqualTo("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til en brevmottaker som ikke har mottakertype bruker med utenlandsk adresse om det allerede finnes en brevmottaker`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(
                    mottakerRolle = FULLMAKT,
                )
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerPersonUtenIdent)
                }
            assertThat(exception.message).isEqualTo(
                "Kan kun legge til bruker med utenlandsk adresse om det finnes en brevmottaker allerede for ${behandling.id}.",
            )
        }

        @EnumSource(value = MottakerRolle::class, names = ["FULLMAKT", "INSTITUSJON"], mode = EXCLUDE)
        @ParameterizedTest
        fun `skal kaste exception om man prøver å legge til en brevmottaker som ikke er FULLMAKT hvis en brevmottaker har mottakerrolle INSTITUSJON`(mottakerRolle: MottakerRolle) {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(mottakerRolle = mottakerRolle)

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent(mottakerRolle = BRUKER)
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            val brevSlot = slot<Brev>()

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
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }

            val capturedBrev = brevSlot.captured
            assertThat(capturedBrev.mottakere?.organisasjoner).isEmpty()
            assertThat(capturedBrev.mottakere?.personer?.filterIsInstance<BrevmottakerPersonMedIdent>()).containsExactly(brevmottakerPersonMedIdent)
        }

        @EnumSource(value = MottakerRolle::class, names = ["INSTITUSJON"], mode = EXCLUDE)
        @ParameterizedTest
        fun `skal opprette brevmottaker når det allerede finnes brevmottakere`(mottakerRolle: MottakerRolle) {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = mottakerRolle,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent(mottakerRolle = BRUKER)
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            val brevSlot = slot<Brev>()

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
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }

            val capturedBrev = brevSlot.captured
            assertThat(capturedBrev.mottakere?.organisasjoner).isEmpty()
            assertThat(capturedBrev.mottakere?.personer?.filterIsInstance<BrevmottakerPersonMedIdent>()).containsExactly(brevmottakerPersonMedIdent)
        }

        @EnumSource(value = MottakerRolle::class, names = ["INSTITUSJON"], mode = EXCLUDE)
        @ParameterizedTest
        fun `skal opprette brevmottaker når brevmottakere i brev er null`(mottakerRolle: MottakerRolle) {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
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
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }
        }

        @EnumSource(value = MottakerRolle::class, names = ["INSTITUSJON"], mode = EXCLUDE)
        @ParameterizedTest
        fun `skal opprette brevmottaker når brevmottakere i brev er tom`(mottakerRolle: MottakerRolle) {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
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
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO", "INSTITUSJON"],
            mode = EXCLUDE,
        )
        @ParameterizedTest
        fun `skal opprette brevmottaker selv om personopplysningsnavnet er forskjellig for mottakertyper som ikke skal være preutfylt`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = mottakerRolle,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent(mottakerRolle = BRUKER)
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
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }
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
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = mottakerRolle,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent(mottakerRolle = BRUKER)
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER_MED_UTENLANDSK_ADRESSE,
                )
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }
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
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = BRUKER_MED_UTENLANDSK_ADRESSE,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(
                    mottakerRolle = mottakerRolle,
                )
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
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
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO"],
            mode = EnumSource.Mode.INCLUDE,
        )
        @ParameterizedTest
        fun `skal slette bruker ved enkelte MottakerRoller`(mottakerRolle: MottakerRolle) {
            // Arrange
            val personIdent = PersonIdent("01010199999")
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = mottakerRolle,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    mottakerRolle = BRUKER,
                    personIdent = personIdent.ident,
                )
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            val brevSlot = slot<Brev>()

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
            } returns DomainUtil.fagsak(identer = setOf(personIdent))

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }

            val capturedBrev = brevSlot.captured
            assertThat(capturedBrev.mottakere?.organisasjoner).isEmpty()
            assertThat(capturedBrev.mottakere?.personer).hasSize(1)
            assertThat(capturedBrev.mottakere?.personer?.filterIsInstance<BrevmottakerPersonUtenIdent>()).anySatisfy {
                assertThat(it).isEqualTo(brevmottaker)
            }
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO"],
            mode = EnumSource.Mode.INCLUDE,
        )
        @ParameterizedTest
        fun `skal ikke slette bruker ved enkelte MottakerRoller når PersonIdent ikke stemmer overens med den aktive identen i fagsaken`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val personIdent = PersonIdent("01010199999")
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerPersonUtenIdent =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                    mottakerRolle = mottakerRolle,
                    navn = "navn",
                    adresselinje1 = "Adresse 1, Mars, 1337",
                    adresselinje2 = null,
                    postnummer = null,
                    poststed = null,
                    landkode = "DK",
                )

            val brevmottakerPersonMedIdent =
                DomainUtil.lagBrevmottakerPersonMedIdent(
                    mottakerRolle = BRUKER,
                    personIdent = "11010199999",
                )
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            val brevSlot = slot<Brev>()

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
            } returns DomainUtil.fagsak(identer = setOf(personIdent))

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerPersonUtenIdent,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottakerPersonUtenIdent.landkode)
            }

            val capturedBrev = brevSlot.captured
            assertThat(capturedBrev.mottakere?.organisasjoner).isEmpty()
            assertThat(capturedBrev.mottakere?.personer).hasSize(2)
            assertThat(capturedBrev.mottakere?.personer?.filterIsInstance<BrevmottakerPersonUtenIdent>()).anySatisfy {
                assertThat(it).isEqualTo(brevmottaker)
            }
            assertThat(capturedBrev.mottakere?.personer?.filterIsInstance<BrevmottakerPersonMedIdent>()).anySatisfy {
                assertThat(it).isEqualTo(brevmottakerPersonMedIdent)
            }
        }
    }

    @Nested
    inner class OpprettBrevmottakerOrganisasjon {
        @Test
        fun `skal kaste exception om behandling ikke er redigerbar`() {
            // Arrange
            val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            val nyBrevmottakerOrganisasjon = DomainUtil.lagNyBrevmottakerOrganisasjon()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerOrganisasjon)
                }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om behandling ikke er i brev steg`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.OPPRETTET)

            val nyBrevmottakerOrganisasjon = DomainUtil.lagNyBrevmottakerOrganisasjon()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerOrganisasjon)
                }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er i steg ${StegType.OPPRETTET}, forventet steg ${StegType.BREV}.")
        }

        @Test
        fun `skal kaste exception om det allerede finnes en brevmottaker med samme MottakerRolle`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerOrganisasjon =
                DomainUtil.lagNyBrevmottakerOrganisasjon(mottakerRolle = FULLMAKT)

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent =
                DomainUtil.lagBrevmottakerPersonUtenIdent(mottakerRolle = FULLMAKT)

            val brevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer = listOf(brevmottakerPersonMedIdent, brevmottakerPersonUtenIdent),
                )

            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto()

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerOrganisasjon)
                }
            assertThat(exception.message).isEqualTo("Kan ikke ha duplikate MottakerRolle. FULLMAKT finnes allerede for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om mottakerrolle ikke er fullmakt`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerOrganisasjon =
                DomainUtil.lagNyBrevmottakerOrganisasjon(mottakerRolle = INSTITUSJON)

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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerOrganisasjon)
                }
            assertThat(
                exception.message,
            ).isEqualTo("Organisasjon kan kun ha mottakerrolle fullmakt for ${behandling.id}.")
        }

        @Test
        fun `skal kaste exception om det allerede finnes en manuelt opprettet brevmottaker når en brevmottaker er institusjon`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerOrganisasjon =
                DomainUtil.lagNyBrevmottakerOrganisasjon(mottakerRolle = FULLMAKT)

            val brevmottakerOrganisasjon = DomainUtil.lagBrevmottakerOrganisasjon(mottakerRolle = INSTITUSJON)
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent(mottakerRolle = VERGE)
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonUtenIdent), organisasjoner = listOf(brevmottakerOrganisasjon))
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
            val exception =
                assertThrows<Feil> {
                    brevmottakerOppretter.opprettBrevmottaker(behandling.id, nyBrevmottakerOrganisasjon)
                }
            assertThat(exception.message).isEqualTo("Kan kun ha én ekstra brevmottaker når institusjon er brevmottaker for ${behandling.id}.")
        }

        @Test
        fun `skal opprette brevmottaker når det allerede finnes brevmottakere`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerOrganisasjon =
                DomainUtil.lagNyBrevmottakerOrganisasjon(mottakerRolle = FULLMAKT)

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent(mottakerRolle = BRUKER)
            val brevmottakere = DomainUtil.lagBrevmottakere(personer = listOf(brevmottakerPersonMedIdent))
            val brev = DomainUtil.lagBrev(mottakere = brevmottakere)

            val brevSlot = slot<Brev>()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto()

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak()

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerOrganisasjon,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerOrganisasjon::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerOrganisasjon.mottakerRolle)
                assertThat(it.organisasjonsnavn).isEqualTo(nyBrevmottakerOrganisasjon.organisasjonsnavn)
                assertThat(it.organisasjonsnummer).isEqualTo(nyBrevmottakerOrganisasjon.organisasjonsnummer)
                assertThat(it.navnHosOrganisasjon).isEqualTo(nyBrevmottakerOrganisasjon.navnHosOrganisasjon)
            }

            val capturedBrev = brevSlot.captured
            assertThat(capturedBrev.mottakere?.organisasjoner).containsExactly(brevmottaker as BrevmottakerOrganisasjon)
            assertThat(capturedBrev.mottakere?.personer).containsExactly(brevmottakerPersonMedIdent)
        }

        @Test
        fun `skal opprette brevmottaker når brevmottakere i brev er null`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerOrganisasjon =
                DomainUtil.lagNyBrevmottakerOrganisasjon(mottakerRolle = FULLMAKT)

            val brev = DomainUtil.lagBrev(mottakere = null)

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto()

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak()

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerOrganisasjon,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerOrganisasjon::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerOrganisasjon.mottakerRolle)
                assertThat(it.organisasjonsnavn).isEqualTo(nyBrevmottakerOrganisasjon.organisasjonsnavn)
                assertThat(it.organisasjonsnummer).isEqualTo(nyBrevmottakerOrganisasjon.organisasjonsnummer)
                assertThat(it.navnHosOrganisasjon).isEqualTo(nyBrevmottakerOrganisasjon.navnHosOrganisasjon)
            }
        }

        @Test
        fun `skal opprette brevmottaker når brevmottakere i brev er tom`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val nyBrevmottakerOrganisasjon =
                DomainUtil.lagNyBrevmottakerOrganisasjon(mottakerRolle = FULLMAKT)

            val brev = DomainUtil.lagBrev(mottakere = DomainUtil.lagBrevmottakere(organisasjoner = emptyList()))

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto()

            every {
                brevService.hentBrev(behandling.id)
            } returns brev

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak()

            every {
                brevRepository.update(any())
            } returnsArgument 0

            // Act
            val brevmottaker =
                brevmottakerOppretter.opprettBrevmottaker(
                    behandling.id,
                    nyBrevmottakerOrganisasjon,
                )

            // Assert
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerOrganisasjon::class.java) {
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottakerOrganisasjon.mottakerRolle)
                assertThat(it.organisasjonsnavn).isEqualTo(nyBrevmottakerOrganisasjon.organisasjonsnavn)
                assertThat(it.organisasjonsnummer).isEqualTo(nyBrevmottakerOrganisasjon.organisasjonsnummer)
                assertThat(it.navnHosOrganisasjon).isEqualTo(nyBrevmottakerOrganisasjon.navnHosOrganisasjon)
            }
        }
    }
}
