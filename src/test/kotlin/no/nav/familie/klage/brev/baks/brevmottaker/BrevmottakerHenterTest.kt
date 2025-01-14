package no.nav.familie.klage.brev.baks.brevmottaker

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerHenterTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()
    private val brevmottakerHenter: BrevmottakerHenter = BrevmottakerHenter(
        brevmottakerRepository = brevmottakerRepository,
    )

    @Nested
    inner class HentBrevmottakereTest {
        @Test
        fun `skal returnere en liste av brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brevmottaker1 = DomainUtil.lagBrevmottaker(
                behandlingId = behandlingId,
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
                adresselinje1 = "Marsveien 1, 0000, Mars",
                postnummer = null,
                poststed = null,
                landkode = "DK",
            )

            val brevmottaker2 = DomainUtil.lagBrevmottaker(
                behandlingId = behandlingId,
                mottakertype = Mottakertype.FULLMEKTIG,
                adresselinje1 = "Osloveien 1",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns listOf(brevmottaker1, brevmottaker2)

            // Act
            val brevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere).containsExactly(brevmottaker1, brevmottaker2)
        }

        @Test
        fun `skal returnere en tom liste av brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            every {
                brevmottakerRepository.findByBehandlingId(behandlingId)
            } returns emptyList()

            // Act
            val brevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere).isEmpty()
        }
    }
}
