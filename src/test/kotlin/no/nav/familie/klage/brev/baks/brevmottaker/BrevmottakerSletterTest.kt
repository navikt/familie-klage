package no.nav.familie.klage.brev.baks.brevmottaker

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class BrevmottakerSletterTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val brevmottakerSletter: BrevmottakerSletter = BrevmottakerSletter(
        brevmottakerRepository = brevmottakerRepository,
        behandlingService = behandlingService,
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
        fun `skal kaste exception om behandlingen er låst for videre redigering`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val brevmottakerId = UUID.randomUUID()

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
            }
            assertThat(exception.message).isEqualTo("Behandling $behandlingId er låst for videre behandling.")
        }

        @Test
        fun `skal kaste exception om man prøver å slette en brevmottaker som ikke eksiterer`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val brevmottakerId = UUID.randomUUID()

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                brevmottakerRepository.existsById(brevmottakerId)
            } returns false

            // Act & assert
            val exception = assertThrows<Feil> {
                brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
            }
            assertThat(exception.message).isEqualTo("Brevmottaker $brevmottakerId kan ikke slettes da den ikke finnes.")
        }

        @Test
        fun `skal slette brevmottaker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val brevmottakerId = UUID.randomUUID()

            every {
                behandlingService.hentBehandling(behandlingId)
            } returns DomainUtil.behandling()

            every {
                brevmottakerRepository.existsById(brevmottakerId)
            } returns true

            every {
                brevmottakerRepository.deleteById(brevmottakerId)
            } just Runs

            // Act
            brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)

            // Assert
            verify(exactly = 1) { brevmottakerRepository.deleteById(brevmottakerId) }
        }
    }
}
