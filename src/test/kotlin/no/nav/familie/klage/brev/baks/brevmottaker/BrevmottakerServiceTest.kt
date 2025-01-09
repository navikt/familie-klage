package no.nav.familie.klage.brev.baks.brevmottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerServiceTest {
    private val brevmottakerHenter: BrevmottakerHenter = mockk()
    private val brevmottakerOppretter: BrevmottakerOppretter = mockk()
    private val brevmottakerSletter: BrevmottakerSletter = mockk()

    private val brevmottakerService: BrevmottakerService = BrevmottakerService(
        brevmottakerHenter = brevmottakerHenter,
        brevmottakerOppretter = brevmottakerOppretter,
        brevmottakerSletter = brevmottakerSletter,
    )

    @Nested
    inner class HentBrevmottakereTest {
        @Test
        fun `skal hente brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brevmottaker1 = DomainUtil.lagBrevmottaker()
            val brevmottaker2 = DomainUtil.lagBrevmottaker()

            every {
                brevmottakerHenter.hentBrevmottakere(behandlingId)
            } returns listOf(brevmottaker1, brevmottaker2)

            // Act
            val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere).containsExactly(brevmottaker1, brevmottaker2)
        }
    }

    @Nested
    inner class OpprettBrevmottakerTeste {
        @Test
        fun `skal opprette brevmottaker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

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
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            } returns DomainUtil.lagBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                mottakertype = nyBrevmottaker.mottakertype,
                navn = nyBrevmottaker.navn,
                adresselinje1 = nyBrevmottaker.adresselinje1,
                adresselinje2 = nyBrevmottaker.adresselinje2,
                postnummer = nyBrevmottaker.postnummer,
                poststed = nyBrevmottaker.poststed,
                landkode = nyBrevmottaker.landkode,
            )

            // Act
            val brevmottaker = brevmottakerService.opprettBrevmottaker(behandlingId, nyBrevmottaker)

            // Assert
            assertThat(brevmottaker.id).isNotNull()
            assertThat(brevmottaker.behandlingId).isEqualTo(behandlingId)
            assertThat(brevmottaker.mottakertype).isEqualTo(nyBrevmottaker.mottakertype)
            assertThat(brevmottaker.navn).isEqualTo(nyBrevmottaker.navn)
            assertThat(brevmottaker.adresselinje1).isEqualTo(nyBrevmottaker.adresselinje1)
            assertThat(brevmottaker.adresselinje2).isEqualTo(nyBrevmottaker.adresselinje2)
            assertThat(brevmottaker.postnummer).isEqualTo(nyBrevmottaker.postnummer)
            assertThat(brevmottaker.poststed).isEqualTo(nyBrevmottaker.poststed)
            assertThat(brevmottaker.landkode).isEqualTo(nyBrevmottaker.landkode)
            assertThat(brevmottaker.sporbar.opprettetAv).isEqualTo("VL")
            assertThat(brevmottaker.sporbar.opprettetTid).isNotNull()
            assertThat(brevmottaker.sporbar.endret.endretAv).isEqualTo("VL")
            assertThat(brevmottaker.sporbar.endret.endretTid).isNotNull()
        }
    }

    @Nested
    inner class SlettBrevmottakerTest {
        @Test
        fun `skal opprette brevmottaker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val brevmottakerId = UUID.randomUUID()

            every {
                brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
            } just runs

            // Act
            brevmottakerService.slettBrevmottaker(behandlingId, brevmottakerId)

            // Assert
            verify(exactly = 1) { brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId) }
        }
    }
}
