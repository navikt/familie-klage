package no.nav.familie.klage.brevmottaker

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerHenterTest {
    private val brevService: BrevService = mockk()
    private val brevmottakerHenter: BrevmottakerHenter = BrevmottakerHenter(brevService)

    @Nested
    inner class HentBrevmottakereTest {
        @Test
        fun `skal håndtere at brevmottakere på brev er null`() {
            // Arrange
            val behandlingId = UUID.randomUUID()
            val brev = DomainUtil.lagBrev(behandlingId = behandlingId, mottakere = null)

            every { brevService.hentBrev(behandlingId) } returns brev

            // Act
            val brevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(brevmottakere.organisasjoner).isEmpty()
            assertThat(brevmottakere.personer).isEmpty()
        }

        @Test
        fun `skal hente brevmottaker`() {
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
            val brev = DomainUtil.lagBrev(behandlingId = behandlingId, mottakere = brevmottakere)

            every { brevService.hentBrev(behandlingId) } returns brev

            // Act
            val hentetBrevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(hentetBrevmottakere.organisasjoner).isEmpty()
            assertThat(hentetBrevmottakere.personer).hasSize(2)
            assertThat(hentetBrevmottakere.personer.filterIsInstance<BrevmottakerPersonMedIdent>()).anySatisfy {
                assertThat(it.personIdent).isEqualTo(brevmottakerPersonMedIdent.personIdent)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonMedIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonMedIdent.navn)
            }
            assertThat(hentetBrevmottakere.personer.filterIsInstance<BrevmottakerPersonUtenIdent>()).anySatisfy {
                assertThat(it.id).isEqualTo(brevmottakerPersonUtenIdent.id)
                assertThat(it.mottakerRolle).isEqualTo(brevmottakerPersonUtenIdent.mottakerRolle)
                assertThat(it.navn).isEqualTo(brevmottakerPersonUtenIdent.navn)
                assertThat(it.adresselinje1).isEqualTo(brevmottakerPersonUtenIdent.adresselinje1)
                assertThat(it.adresselinje2).isEqualTo(brevmottakerPersonUtenIdent.adresselinje2)
                assertThat(it.postnummer).isEqualTo(brevmottakerPersonUtenIdent.postnummer)
                assertThat(it.poststed).isEqualTo(brevmottakerPersonUtenIdent.poststed)
                assertThat(it.landkode).isEqualTo(brevmottakerPersonUtenIdent.landkode)
            }
        }
    }
}
