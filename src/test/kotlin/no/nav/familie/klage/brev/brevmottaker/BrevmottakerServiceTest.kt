package no.nav.familie.klage.brev.brevmottaker

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

class BrevmottakerServiceTest {
    private val brevmottakerHenter: BrevmottakerHenter = mockk()
    private val brevmottakerErstatter: BrevmottakerErstatter = mockk()
    private val brevmottakerOppretter: BrevmottakerOppretter = mockk()
    private val brevmottakerSletter: BrevmottakerSletter = mockk()

    private val brevmottakerService: BrevmottakerService = BrevmottakerService(
        brevmottakerHenter = brevmottakerHenter,
        brevmottakerErstatter = brevmottakerErstatter,
        brevmottakerOppretter = brevmottakerOppretter,
        brevmottakerSletter = brevmottakerSletter,
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
                brevmottakerService.hentBrevmottakere(behandlingId)
            } returns brevmottakere

            // Act
            val hentetBrevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(hentetBrevmottakere.organisasjoner).isEmpty()
            assertThat(hentetBrevmottakere.personer).containsExactly(
                brevmottakerPersonMedIdent,
                brevmottakerPersonUtenIdent,
            )
        }
    }

    @Nested
    inner class ErstattBrevmottakereTest {
        @Test
        fun `skal erstatte brevmottakere`() {
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
                brevmottakerService.erstattBrevmottakere(behandlingId, brevmottakere)
            } returns brevmottakere

            // Act
            val hentetBrevmottakere = brevmottakerService.erstattBrevmottakere(behandlingId, brevmottakere)

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
                brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
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
            val brevmottaker = brevmottakerService.opprettBrevmottaker(behandlingId, nyBrevmottaker)

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
                brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
            } just runs

            // Act
            brevmottakerService.slettBrevmottaker(behandlingId, brevmottakerId)

            // Assert
            verify(exactly = 1) { brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId) }
        }
    }
}
