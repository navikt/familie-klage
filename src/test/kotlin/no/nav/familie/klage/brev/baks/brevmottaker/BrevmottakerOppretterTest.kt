package no.nav.familie.klage.brev.baks.brevmottaker

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.klage.behandling.BehandlingService
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
import java.util.UUID

class BrevmottakerOppretterTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()

    private val brevmottakerOppretter: BrevmottakerOppretter = BrevmottakerOppretter(
        behandlingService = behandlingService,
        personopplysningerService = personopplysningerService,
        brevmottakerRepository = brevmottakerRepository,
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
        fun `skal kaste exception om behandlingen er låst for videre redigering`() {
            // Arrange
            val behandlingId: UUID = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker()

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Behandling $behandlingId er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om det allerede finnes en brevmottaker med samme mottakertype`() {
            // Arrange
            val behandlingId: UUID = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.BRUKER,
                navn = "navn",
                adresselinje1 = "adresse1",
                adresselinje2 = "adresse2",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                personopplysningerService.hentPersonopplysninger(behandlingId)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottaker.navn)

            every {
                brevmottakerRepository.insert(any())
            } returnsArgument 0

            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns listOf(DomainUtil.lagBrevmottaker(mottakertype = nyBrevmottaker.mottakertype))

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Kan ikke ha duplikate mottakertyper. BRUKER finnes allerede.")
        }

        @Test
        fun `skal kaste exception om bruker med utenlandsk adresse ikke har samme navn som i personopplysningene`() {
            // Arrange
            val behandlingId: UUID = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "navn",
                adresselinje1 = "adresse1",
                postnummer = null,
                poststed = null,
                landkode = "BR",
            )

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                personopplysningerService.hentPersonopplysninger(behandlingId)
            } returns DomainUtil.lagPersonopplysningerDto(navn = "ikke samme navn")

            every {
                brevmottakerRepository.insert(any())
            } returnsArgument 0

            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns emptyList()

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
        }

        @Test
        fun `skal kaste exception om dødsbo ikke har samme navn som i personopplysningene`() {
            // Arrange
            val behandlingId: UUID = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.DØDSBO,
                navn = "navn",
                adresselinje1 = "adresse1",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                personopplysningerService.hentPersonopplysninger(behandlingId)
            } returns DomainUtil.lagPersonopplysningerDto(navn = "ikke samme navn")

            every {
                brevmottakerRepository.insert(any())
            } returnsArgument 0

            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns emptyList()

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn.")
        }

        @Test
        fun `skal kaste exception om man prøver å legge til dødsbo når det allerede finnes en brevmottaker`() {
            // Arrange
            val behandlingId: UUID = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.DØDSBO,
                navn = "navn",
                adresselinje1 = "adresse1",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                personopplysningerService.hentPersonopplysninger(behandlingId)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottaker.navn)

            every {
                brevmottakerRepository.insert(any())
            } returnsArgument 0

            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns listOf(DomainUtil.lagBrevmottaker())

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            }
            assertThat(exception.message).isEqualTo("Kan ikke legge til dødsbo når det allerede finnes brevmottakere.")
        }

        @Test
        fun `skal opprette brevmottaker`() {
            // Arrange
            val behandlingId: UUID = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.BRUKER,
                navn = "navn",
                adresselinje1 = "adresse1",
                adresselinje2 = "adresse2",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                personopplysningerService.hentPersonopplysninger(behandlingId)
            } returns DomainUtil.lagPersonopplysningerDto(navn = nyBrevmottaker.navn)

            every {
                brevmottakerRepository.insert(any())
            } returnsArgument 0

            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns emptyList()

            // Act
            val brevmottaker = brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)

            // Assert
            assertThat(brevmottaker.id).isNotNull()
            assertThat(brevmottaker.behandlingId).isEqualTo(behandlingId)
            assertThat(brevmottaker.mottakertype).isEqualTo(brevmottaker.mottakertype)
            assertThat(brevmottaker.navn).isEqualTo(brevmottaker.navn)
            assertThat(brevmottaker.adresselinje1).isEqualTo(brevmottaker.adresselinje1)
            assertThat(brevmottaker.adresselinje2).isEqualTo(brevmottaker.adresselinje2)
            assertThat(brevmottaker.postnummer).isEqualTo(brevmottaker.postnummer)
            assertThat(brevmottaker.poststed).isEqualTo(brevmottaker.poststed)
            assertThat(brevmottaker.landkode).isEqualTo(brevmottaker.landkode)
        }
    }
}
