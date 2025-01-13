package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.testutil.DtoTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NyBrevmottakerDtoMapperTest {
    @Nested
    inner class TilDomeneTest {
        @Test
        fun `skal mappe til domene`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                mottakertype = Mottakertype.FULLMEKTIG,
                navn = "Navn Navnesen",
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                postnummer = "0010",
                poststed = "Oslo",
                landkode = "NO",
            )

            // Act
            val nyBrevmottaker = NyBrevmottakerDtoMapper.tilDomene(nyBrevmottakerDto)

            // Assert
            assertThat(nyBrevmottaker.mottakertype).isEqualTo(nyBrevmottakerDto.mottakertype)
            assertThat(nyBrevmottaker.navn).isEqualTo(nyBrevmottakerDto.navn)
            assertThat(nyBrevmottaker.adresselinje1).isEqualTo(nyBrevmottakerDto.adresselinje1)
            assertThat(nyBrevmottaker.adresselinje2).isEqualTo(nyBrevmottakerDto.adresselinje2)
            assertThat(nyBrevmottaker.postnummer).isEqualTo(nyBrevmottakerDto.postnummer)
            assertThat(nyBrevmottaker.poststed).isEqualTo(nyBrevmottakerDto.poststed)
            assertThat(nyBrevmottaker.landkode).isEqualTo(nyBrevmottakerDto.landkode)
        }
    }
}
