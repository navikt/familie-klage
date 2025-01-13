package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.testutil.DtoTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class NyBrevmottakerDtoTest {
    @Nested
    inner class ValiderTest {
        @Test
        fun `skal kaste exception om mottakertype er bruker`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(mottakertype = Mottakertype.BRUKER)

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Det er ikke mulig å sette ${Mottakertype.BRUKER} for saksbehandler.")
        }

        @Test
        fun `skal kaste exception om landkode er mindre enn 2 tegn`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "N")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ugyldig landkode: N.")
        }

        @Test
        fun `skal kaste exception om landkode er mer enn 2 tegn`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "NOR")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ugyldig landkode: NOR.")
        }

        @Test
        fun `skal kaste exception om navn er en tom string`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(navn = "")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om navn er en string med kun mellomrom`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(navn = " ")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om adresselinje1 er en tom string`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(adresselinje1 = "")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Adresselinje 1 kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om adresselinje1 er en string med kun mellomrom`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(adresselinje1 = " ")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Adresselinje 1 kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om postnummer er null og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "NO", postnummer = null)

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Når landkode er NO (Norge) må postnummer være satt.")
        }

        @Test
        fun `skal kaste exception om poststed er null og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "NO", poststed = null)

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Når landkode er NO (Norge) må poststed være satt.")
        }

        @Test
        fun `skal kaste exception om postnummer ikke inneholder kun tall og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "NO", postnummer = "123T")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om postnummer er mindre enn 4 tegn og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "NO", postnummer = "123")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om postnummer er mer enn 4 tegn og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(landkode = "NO", postnummer = "12345")

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om mottakertype er bruker med utenlandsk adresse og landkode er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                landkode = "NO",
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Bruker med utenlandsk adresse kan ikke ha landkode NO.")
        }

        @Test
        fun `skal kaste exception om postnummer ikke er null og landkode ikke er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                landkode = "BR",
                postnummer = "1234",
                poststed = null,
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ved utenlandsk landkode må postnummer settes i adresselinje 1.")
        }

        @Test
        fun `skal kaste exception om poststed ikke er null og landkode ikke er NO`() {
            // Arrange
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                landkode = "BR",
                postnummer = null,
                poststed = "Harstad",
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                nyBrevmottakerDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("Ved utenlandsk landkode må poststed settes i adresselinje 1.")
        }

        @Test
        fun `skal opprette ny brevmottaker med utenlandsk landekode`() {
            // Act
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "Navn Navnesen",
                adresselinje1 = "Adresseveien 1, Danmark, 0010",
                adresselinje2 = null,
                landkode = "DK",
                postnummer = null,
                poststed = null,
            )

            // Assert
            assertThat(nyBrevmottakerDto.mottakertype).isEqualTo(Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE)
            assertThat(nyBrevmottakerDto.navn).isEqualTo("Navn Navnesen")
            assertThat(nyBrevmottakerDto.adresselinje1).isEqualTo("Adresseveien 1, Danmark, 0010")
            assertThat(nyBrevmottakerDto.adresselinje2).isNull()
            assertThat(nyBrevmottakerDto.postnummer).isNull()
            assertThat(nyBrevmottakerDto.poststed).isNull()
            assertThat(nyBrevmottakerDto.landkode).isEqualTo("DK")
        }

        @Test
        fun `skal opprette ny brevmottaker med norsk landekode`() {
            // Act
            val nyBrevmottakerDto = DtoTestUtil.lagNyBrevmottakerDto(
                mottakertype = Mottakertype.FULLMEKTIG,
                navn = "Navn Navnesen",
                adresselinje1 = "Adresseveien 1",
                adresselinje2 = null,
                landkode = "NO",
                postnummer = "0010",
                poststed = "Oslo",
            )

            // Assert
            assertThat(nyBrevmottakerDto.mottakertype).isEqualTo(Mottakertype.FULLMEKTIG)
            assertThat(nyBrevmottakerDto.navn).isEqualTo("Navn Navnesen")
            assertThat(nyBrevmottakerDto.adresselinje1).isEqualTo("Adresseveien 1")
            assertThat(nyBrevmottakerDto.adresselinje2).isNull()
            assertThat(nyBrevmottakerDto.postnummer).isEqualTo("0010")
            assertThat(nyBrevmottakerDto.poststed).isEqualTo("Oslo")
            assertThat(nyBrevmottakerDto.landkode).isEqualTo("NO")
        }
    }
}
