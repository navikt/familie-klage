package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerDtoMapperTest {
    @Nested
    inner class TilDtoTest {
        @Test
        fun `skal mappe fra domene til dto`() {
            // Arrange
            val brevmottakerId = UUID.randomUUID()
            val behandlingId = UUID.randomUUID()

            val brevmottaker = DomainUtil.lagBrevmottaker(
                id = brevmottakerId,
                behandlingId = behandlingId,
                mottakertype = Mottakertype.BRUKER,
                navn = "Navn",
                adresselinje1 = "Adresseline1",
                postnummer = "1234",
                poststed = "Oslo",
                landkode = "NO",
            )

            // Act
            val brevmottakerDto = BrevmottakerDtoMapper.tilDto(brevmottaker)

            // Assert
            assertThat(brevmottakerDto.id).isEqualTo(brevmottaker.id)
            assertThat(brevmottakerDto.mottakertype).isEqualTo(brevmottaker.mottakertype)
            assertThat(brevmottakerDto.navn).isEqualTo(brevmottaker.navn)
            assertThat(brevmottakerDto.adresselinje1).isEqualTo(brevmottaker.adresselinje1)
            assertThat(brevmottakerDto.adresselinje2).isEqualTo(brevmottaker.adresselinje2)
            assertThat(brevmottakerDto.postnummer).isEqualTo(brevmottaker.postnummer)
            assertThat(brevmottakerDto.poststed).isEqualTo(brevmottaker.poststed)
            assertThat(brevmottakerDto.landkode).isEqualTo(brevmottaker.landkode)
        }
    }
}
