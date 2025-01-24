package no.nav.familie.klage.brev.brevmottaker.baks

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BaksBrevmottakerServiceTest {
    private val baksBrevmottakerHenter: BaksBrevmottakerHenter = mockk()
    private val baksBrevmottakerOppretter: BaksBrevmottakerOppretter = mockk()
    private val baksBrevmottakerSletter: BaksBrevmottakerSletter = mockk()

    private val baksBrevmottakerService: BaksBrevmottakerService = BaksBrevmottakerService(
        baksBrevmottakerHenter = baksBrevmottakerHenter,
        baksBrevmottakerOppretter = baksBrevmottakerOppretter,
        baksBrevmottakerSletter = baksBrevmottakerSletter,
    )

    @Nested
    inner class HentBrevmottakereTest {
        @Test
        fun `skal hente brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent()
            val brevmottakere = DomainUtil.lagBrevmottakere(
                personer = listOf(
                    brevmottakerPersonMedIdent,
                    brevmottakerPersonUtenIdent,
                ),
            )

            every {
                baksBrevmottakerService.hentBrevmottakere(behandlingId)
            } returns brevmottakere

            // Act
            val hentetBrevmottakere = baksBrevmottakerService.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(hentetBrevmottakere.organisasjoner).isEmpty()
            assertThat(hentetBrevmottakere.personer).containsExactly(
                brevmottakerPersonMedIdent,
                brevmottakerPersonUtenIdent,
            )
        }
    }

    @Nested
    inner class OpprettBrevmottakerTeste {
        @Test
        fun `skal opprette brevmottaker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottakerPersonUtenIdent(
                mottakerRolle = MottakerRolle.BRUKER,
                navn = "navn",
                adresselinje1 = "adresse1",
                adresselinje2 = "adresse2",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            every {
                baksBrevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
            } returns DomainUtil.lagBrevmottakerPersonUtenIdent(
                id = UUID.randomUUID(),
                mottakerRolle = nyBrevmottaker.mottakerRolle,
                navn = nyBrevmottaker.navn,
                adresselinje1 = nyBrevmottaker.adresselinje1,
                adresselinje2 = nyBrevmottaker.adresselinje2,
                postnummer = nyBrevmottaker.postnummer,
                poststed = nyBrevmottaker.poststed,
                landkode = nyBrevmottaker.landkode,
            )

            // Act
            val brevmottaker = baksBrevmottakerService.opprettBrevmottaker(behandlingId, nyBrevmottaker)

            // Assert
            assertThat(brevmottaker.id).isNotNull()
            assertThat(brevmottaker.mottakerRolle).isEqualTo(nyBrevmottaker.mottakerRolle)
            assertThat(brevmottaker.navn).isEqualTo(nyBrevmottaker.navn)
            assertThat(brevmottaker.adresselinje1).isEqualTo(nyBrevmottaker.adresselinje1)
            assertThat(brevmottaker.adresselinje2).isEqualTo(nyBrevmottaker.adresselinje2)
            assertThat(brevmottaker.postnummer).isEqualTo(nyBrevmottaker.postnummer)
            assertThat(brevmottaker.poststed).isEqualTo(nyBrevmottaker.poststed)
            assertThat(brevmottaker.landkode).isEqualTo(nyBrevmottaker.landkode)
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
                baksBrevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
            } just runs

            // Act
            baksBrevmottakerService.slettBrevmottaker(behandlingId, brevmottakerId)

            // Assert
            verify(exactly = 1) { baksBrevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId) }
        }
    }
}
