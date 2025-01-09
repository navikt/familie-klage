package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerTest {
    @Nested
    inner class FabrikkTest {
        @Test
        fun `skal opprette Brevmottaker`() {
            // Arrange
            val behandlingId = UUID.randomUUID()

            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.BRUKER,
                navn = "Navn",
                adresselinje1 = "Adresseline1",
                postnummer = "1234",
                poststed = "Oslo",
                landkode = "NO",
            )

            // Act
            val brevmottaker = Brevmottaker.opprett(behandlingId, nyBrevmottaker)

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
}
