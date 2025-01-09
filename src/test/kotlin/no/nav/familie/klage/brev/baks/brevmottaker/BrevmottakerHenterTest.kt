package no.nav.familie.klage.brev.baks.brevmottaker

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerHenterTest {
    private val mockedBrevmottakerRepository: BrevmottakerRepository = mockk()
    private val brevmottakerHenter: BrevmottakerHenter = BrevmottakerHenter(
        brevmottakerRepository = mockedBrevmottakerRepository,
    )

    @Nested
    inner class HentBrevmottakereTest {
        @Test
        fun `skal returnere en liste av brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brevmottaker1 = DomainUtil.lagBrevmottaker(behandlingId = behandlingId)
            val brevmottaker2 = DomainUtil.lagBrevmottaker(behandlingId = behandlingId)
            val brevmottaker3 = DomainUtil.lagBrevmottaker(behandlingId = behandlingId)

            every {
                mockedBrevmottakerRepository.findByBehandlingId(behandlingId)
            } returns listOf(brevmottaker1, brevmottaker2, brevmottaker3)

            // Act
            val brevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere).containsExactly(brevmottaker1, brevmottaker2, brevmottaker3)
        }

        @Test
        fun `skal returnere en tom liste av brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            every {
                mockedBrevmottakerRepository.findByBehandlingId(behandlingId)
            } returns emptyList()

            // Act
            val brevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere).isEmpty()
        }
    }
}
