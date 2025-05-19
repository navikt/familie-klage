package no.nav.familie.klage.brevmottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottakerPersonUtenIdent
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerServiceTest {
    private val brevmottakerHenter = mockk<BrevmottakerHenter>()
    private val brevmottakerErstatter = mockk<BrevmottakerErstatter>()
    private val brevmottakerOppretter = mockk<BrevmottakerOppretter>()
    private val brevmottakerSletter = mockk<BrevmottakerSletter>()
    private val brevmottakerUtleder = mockk<BrevmottakerUtleder>()

    private val brevmottakerService: BrevmottakerService =
        BrevmottakerService(
            brevmottakerHenter = brevmottakerHenter,
            brevmottakerErstatter = brevmottakerErstatter,
            brevmottakerOppretter = brevmottakerOppretter,
            brevmottakerSletter = brevmottakerSletter,
            brevmottakerUtleder = brevmottakerUtleder,
        )

    @Nested
    inner class HentBrevmottakereTest {
        @Test
        fun `skal hente brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brevmottakerPersonMedIdent = DomainUtil.lagBrevmottakerPersonMedIdent()
            val brevmottakerPersonUtenIdent = DomainUtil.lagBrevmottakerPersonUtenIdent()
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
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
            val brevmottakere =
                DomainUtil.lagBrevmottakere(
                    personer =
                        listOf(
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

            val nyBrevmottaker =
                DomainUtil.lagNyBrevmottakerPersonUtenIdent(
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
            } returns
                DomainUtil.lagBrevmottakerPersonUtenIdent(
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
            assertThat(brevmottaker).isInstanceOfSatisfying(BrevmottakerPersonUtenIdent::class.java) {
                assertThat(it.id).isNotNull()
                assertThat(it.mottakerRolle).isEqualTo(nyBrevmottaker.mottakerRolle)
                assertThat(it.navn).isEqualTo(nyBrevmottaker.navn)
                assertThat(it.adresselinje1).isEqualTo(nyBrevmottaker.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(nyBrevmottaker.adresselinje2)
                assertThat(it.postnummer).isEqualTo(nyBrevmottaker.postnummer)
                assertThat(it.poststed).isEqualTo(nyBrevmottaker.poststed)
                assertThat(it.landkode).isEqualTo(nyBrevmottaker.landkode)
            }
        }
    }

    @Nested
    inner class SlettBrevmottakerTest {
        @Test
        fun `skal slette brevmottaker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val slettbarBrevmottaker = SlettbarBrevmottakerPersonUtenIdent(UUID.randomUUID())

            every {
                brevmottakerSletter.slettBrevmottaker(behandlingId, slettbarBrevmottaker)
            } just runs

            // Act
            brevmottakerService.slettBrevmottaker(behandlingId, slettbarBrevmottaker)

            // Assert
            verify(exactly = 1) { brevmottakerSletter.slettBrevmottaker(behandlingId, slettbarBrevmottaker) }
        }
    }

    @Nested
    inner class UtledInitielleBrevmottakere {
        @Test
        fun `skal utlede initielle brevmottakere`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val brevmottakere =
                Brevmottakere(
                    personer =
                        listOf(
                            BrevmottakerPersonMedIdent(
                                personIdent = "123",
                                mottakerRolle = MottakerRolle.BRUKER,
                                navn = "Navn Navnesen",
                            ),
                        ),
                    organisasjoner = emptyList(),
                )

            every { brevmottakerUtleder.utledInitielleBrevmottakere(behandlingId) } returns brevmottakere

            // Act
            val initielleBrevmottakere = brevmottakerService.utledInitielleBrevmottakere(behandlingId)

            // Assert
            assertThat(initielleBrevmottakere).isEqualTo(brevmottakere)
        }
    }
}
