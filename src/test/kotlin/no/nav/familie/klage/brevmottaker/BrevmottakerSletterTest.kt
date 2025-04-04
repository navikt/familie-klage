package no.nav.familie.klage.brevmottaker

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.UUID
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonUtenIdent
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

class BrevmottakerSletterTest {
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val brevService: BrevService = mockk()
    private val brevRepository: BrevRepository = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val brevmottakerSletter: BrevmottakerSletter = BrevmottakerSletter(
        behandlingService = behandlingService,
        brevService = brevService,
        brevRepository = brevRepository,
        fagsakService = fagsakService,
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
    inner class SlettBrevmottakerTest {
        @Test
        fun `skal kaste exception om brevmottaker er SlettbarBrevmottakerOrganisasjon`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val slettbarBrevmottaker = SlettbarBrevmottakerOrganisasjon("123")

            // Act & assert
            val exception = assertThrows<UnsupportedOperationException> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Sletting av organisasjon er ikke støttet.")
        }

        @Test
        fun `skal kaste exception om brevmottaker er SlettbarBrevmottakerPersonMedIdent`() {
            // Arrange
            val behandling = DomainUtil.behandling()

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonMedIdent("123")

            // Act & assert
            val exception = assertThrows<UnsupportedOperationException> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Sletting av person med ident er ikke støttet.")
        }

        @Test
        fun `skal kaste exception om behandling er låst for videre redigering`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV, status = BehandlingStatus.FERDIGSTILT)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Behandling ${behandling.id} er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om behandling er i feil steg`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.FORMKRAV)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Behandlingen er i steg ${behandling.steg}, forventet steg ${StegType.BREV}.")
        }

        @Test
        fun `skal kaste exception om brevmottakeren som skal slettes ikke finnes da mottakere i brev er null`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(behandlingId = behandling.id, mottakere = null)

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Brevmottaker ${slettbarBrevmottaker.id} kan ikke slettes da den ikke finnes.")
        }

        @Test
        fun `skal kaste exception om brevmottakeren som skal slettes ikke finnes da mottakere i brev er en tom liste`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(
                behandlingId = behandling.id,
                mottakere = DomainUtil.lagBrevmottakere(personer = emptyList()),
            )

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Brevmottaker ${slettbarBrevmottaker.id} kan ikke slettes da den ikke finnes.")
        }

        @Test
        fun `skal kaste exception om man prøver å slette slik at ingen brevmottakere er igjen`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(
                identer = setOf(PersonIdent("123")),
            )

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(
                behandlingId = behandling.id,
                mottakere = Brevmottakere(
                    personer = listOf(
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = slettbarBrevmottaker.id,
                            mottakerRolle = MottakerRolle.FULLMAKT,
                        ),
                    ),
                ),
            )

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Må ha minimum en brevmottaker for behandling ${behandling.id}.")
            verify { brevRepository wasNot called }
        }

        @Test
        fun `skal slette brevmottakeren`() {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            val brevSlot = slot<Brev>()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(
                identer = setOf(PersonIdent("123")),
            )

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(
                behandlingId = behandling.id,
                mottakere = Brevmottakere(
                    personer = listOf(
                        DomainUtil.lagBrevmottakerPersonMedIdent(
                            mottakerRolle = MottakerRolle.BRUKER,
                        ),
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = slettbarBrevmottaker.id,
                            mottakerRolle = MottakerRolle.FULLMAKT,
                        ),
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = UUID.randomUUID(),
                            mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                            adresselinje1 = "Adresse 1, Mars, 1337",
                            adresselinje2 = null,
                            poststed = null,
                            postnummer = null,
                            landkode = "DK",
                        ),
                    ),
                ),
            )

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)

            // Assert
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(2)

            val brevmottakerPersonUtenIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonUtenIdent::class.java)
            assertThat(brevmottakerPersonUtenIdent).hasSize(1)
            assertThat(brevmottakerPersonUtenIdent).allSatisfy {
                assertThat(it.id).isNotEqualTo(slettbarBrevmottaker.id)
            }

            val brevmottakerPersonMedIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonMedIdent::class.java)
            assertThat(brevmottakerPersonMedIdent).hasSize(1)
            assertThat(brevmottakerPersonMedIdent).allSatisfy {
                assertThat(it.personIdent).isNotEqualTo(slettbarBrevmottaker.id.toString())
            }

            verify(exactly = 1) { brevRepository.update(any()) }
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        @ParameterizedTest
        fun `skal slette brevmottakeren og ikke legge til bruker selv om bruker ikke allerede eksisterer for visse mottaker roller`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val behandling = DomainUtil.behandling(steg = StegType.BREV)

            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            val brevSlot = slot<Brev>()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(
                identer = setOf(PersonIdent("123")),
            )

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(
                behandlingId = behandling.id,
                mottakere = Brevmottakere(
                    personer = listOf(
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = slettbarBrevmottaker.id,
                            mottakerRolle = mottakerRolle,
                        ),
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = UUID.randomUUID(),
                            mottakerRolle = MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE,
                            adresselinje1 = "Adresse 1, Mars, 1337",
                            adresselinje2 = null,
                            poststed = null,
                            postnummer = null,
                            landkode = "DK",
                        ),
                    ),
                ),
            )

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)

            // Assert
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)

            val brevmottakerPersonUtenIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonUtenIdent::class.java)
            assertThat(brevmottakerPersonUtenIdent).hasSize(1)
            assertThat(brevmottakerPersonUtenIdent).allSatisfy {
                assertThat(it.id).isNotEqualTo(slettbarBrevmottaker.id)
            }

            val brevmottakerPersonMedIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonMedIdent::class.java)
            assertThat(brevmottakerPersonMedIdent).isEmpty()

            verify(exactly = 1) { brevRepository.update(any()) }
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO"],
            mode = EnumSource.Mode.INCLUDE,
        )
        @ParameterizedTest
        fun `skal slette brevmottakeren og legge til bruker som ikke allerede eksisterer for visse mottaker roller`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val personIdent = PersonIdent("123")
            val behandling = DomainUtil.behandling(steg = StegType.BREV)
            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())
            val brevSlot = slot<Brev>()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(
                identer = setOf(personIdent),
            )

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(
                personIdent = personIdent.ident,
                navn = "Navn Etternavn",
            )

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(
                behandlingId = behandling.id,
                mottakere = Brevmottakere(
                    personer = listOf(
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = slettbarBrevmottaker.id,
                            mottakerRolle = mottakerRolle,
                            adresselinje1 = "Adresse 1, Mars, 1337",
                            adresselinje2 = null,
                            poststed = null,
                            postnummer = null,
                            landkode = "DK",
                        ),
                    ),
                ),
            )

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)

            // Assert
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)

            val brevmottakerPersonUtenIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonUtenIdent::class.java)
            assertThat(brevmottakerPersonUtenIdent).isEmpty()

            val brevmottakerPersonMedIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonMedIdent::class.java)
            assertThat(brevmottakerPersonMedIdent).hasSize(1)
            assertThat(brevmottakerPersonMedIdent).allSatisfy {
                assertThat(it.personIdent).isEqualTo(personIdent.ident)
            }

            verify(exactly = 1) { brevRepository.update(any()) }
        }

        @EnumSource(
            value = MottakerRolle::class,
            names = ["BRUKER_MED_UTENLANDSK_ADRESSE", "DØDSBO"],
            mode = EnumSource.Mode.INCLUDE,
        )
        @ParameterizedTest
        fun `skal slette brevmottakeren men legge til bruker som allerede eksisterer for visse mottaker roller`(
            mottakerRolle: MottakerRolle,
        ) {
            // Arrange
            val personIdent = PersonIdent("123")
            val behandling = DomainUtil.behandling(steg = StegType.BREV)
            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())
            val brevSlot = slot<Brev>()

            every {
                behandlingService.hentBehandling(behandling.id)
            } returns behandling

            every {
                fagsakService.hentFagsak(behandling.fagsakId)
            } returns DomainUtil.fagsak(
                identer = setOf(personIdent),
            )

            every {
                personopplysningerService.hentPersonopplysninger(behandling.id)
            } returns DomainUtil.lagPersonopplysningerDto(
                personIdent = personIdent.ident,
                navn = "Navn Etternavn",
            )

            every {
                brevService.hentBrev(behandling.id)
            } returns DomainUtil.lagBrev(
                behandlingId = behandling.id,
                mottakere = Brevmottakere(
                    personer = listOf(
                        DomainUtil.lagBrevmottakerPersonMedIdent(
                            personIdent = personIdent.ident,
                            mottakerRolle = MottakerRolle.BRUKER,
                        ),
                        DomainUtil.lagBrevmottakerPersonUtenIdent(
                            id = slettbarBrevmottaker.id,
                            mottakerRolle = mottakerRolle,
                            adresselinje1 = "Adresse 1, Mars, 1337",
                            adresselinje2 = null,
                            poststed = null,
                            postnummer = null,
                            landkode = "DK",
                        ),
                    ),
                ),
            )

            every {
                brevRepository.update(capture(brevSlot))
            } returnsArgument 0

            // Act
            brevmottakerSletter.slettBrevmottaker(behandling.id, slettbarBrevmottaker)

            // Assert
            assertThat(brevSlot.captured.mottakere?.organisasjoner).isEmpty()
            assertThat(brevSlot.captured.mottakere?.personer).hasSize(1)

            val brevmottakerPersonUtenIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonUtenIdent::class.java)
            assertThat(brevmottakerPersonUtenIdent).isEmpty()

            val brevmottakerPersonMedIdent = brevSlot
                .captured
                .mottakere
                ?.personer
                ?.filterIsInstance(BrevmottakerPersonMedIdent::class.java)
            assertThat(brevmottakerPersonMedIdent).hasSize(1)
            assertThat(brevmottakerPersonMedIdent).allSatisfy {
                assertThat(it.personIdent).isEqualTo(personIdent.ident)
            }

            verify(exactly = 1) { brevRepository.update(any()) }
        }
    }
}
