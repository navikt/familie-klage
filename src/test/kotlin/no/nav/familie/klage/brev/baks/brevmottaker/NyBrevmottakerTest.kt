package no.nav.familie.klage.brev.baks.brevmottaker

import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NyBrevmottakerTest {
    @Nested
    inner class InitTest {
        @Test
        fun `skal kaste exception om landkode er mindre enn 2 tegn`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "N")
            }
            assertThat(exception.message).isEqualTo("Ugyldig landkode: N.")
        }

        @Test
        fun `skal kaste exception om landkode er mer enn 2 tegn`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NOR")
            }
            assertThat(exception.message).isEqualTo("Ugyldig landkode: NOR.")
        }

        @Test
        fun `skal kaste exception om navn er en tom string`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(navn = "")
            }
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om navn er en string med kun mellomrom`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(navn = " ")
            }
            assertThat(exception.message).isEqualTo("Navn kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om adresselinje1 er en tom string`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(adresselinje1 = "")
            }
            assertThat(exception.message).isEqualTo("Adresselinje1 kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om adresselinje1 er en string med kun mellomrom`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(adresselinje1 = " ")
            }
            assertThat(exception.message).isEqualTo("Adresselinje1 kan ikke være tomt.")
        }

        @Test
        fun `skal kaste exception om postnummer er null og landkode er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NO", postnummer = null)
            }
            assertThat(exception.message).isEqualTo("Når landkode er NO (Norge) må postnummer være satt.")
        }

        @Test
        fun `skal kaste exception om poststed er null og landkode er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NO", poststed = null)
            }
            assertThat(exception.message).isEqualTo("Når landkode er NO (Norge) må poststed være satt.")
        }

        @Test
        fun `skal kaste exception om postnummer ikke inneholder kun tall og landkode er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NO", postnummer = "123T")
            }
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om postnummer er mindre enn 4 tegn og landkode er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NO", postnummer = "123")
            }
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om postnummer er mer enn 4 tegn og landkode er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NO", postnummer = "12345")
            }
            assertThat(exception.message).isEqualTo("Postnummer må være 4 siffer.")
        }

        @Test
        fun `skal kaste exception om mottakertype er bruker med utenlandsk adresse og landkode er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "NO", mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE)
            }
            assertThat(exception.message).isEqualTo("Bruker med utenlandsk adresse kan ikke ha landkode NO.")
        }

        @Test
        fun `skal kaste exception om postnummer ikke er null og landkode ikke er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "BR", postnummer = "1234", poststed = null)
            }
            assertThat(exception.message).isEqualTo("Ved utenlandsk landkode må postnummer settes i adresselinje 1.")
        }

        @Test
        fun `skal kaste exception om poststed ikke er null og landkode ikke er NO`() {
            // Act & assert
            val exception = assertThrows<IllegalStateException> {
                DomainUtil.lagNyBrevmottaker(landkode = "BR", postnummer = null, poststed = "Harstad")
            }
            assertThat(exception.message).isEqualTo("Ved utenlandsk landkode må poststed settes i adresselinje 1.")
        }

        @Test
        fun `skal opprette ny brevmottaker med utenlandsk landekode`() {
            // Act
            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE,
                navn = "Navn Navnesen",
                adresselinje1 = "Adresseveien 1, Danmark, 0010",
                adresselinje2 = null,
                landkode = "DK",
                postnummer = null,
                poststed = null,
            )

            // Assert
            assertThat(nyBrevmottaker.mottakertype).isEqualTo(Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE)
            assertThat(nyBrevmottaker.navn).isEqualTo("Navn Navnesen")
            assertThat(nyBrevmottaker.adresselinje1).isEqualTo("Adresseveien 1, Danmark, 0010")
            assertThat(nyBrevmottaker.adresselinje2).isNull()
            assertThat(nyBrevmottaker.postnummer).isNull()
            assertThat(nyBrevmottaker.poststed).isNull()
            assertThat(nyBrevmottaker.landkode).isEqualTo("DK")
        }

        @Test
        fun `skal opprette ny brevmottaker med norsk landekode`() {
            // Act
            val nyBrevmottaker = DomainUtil.lagNyBrevmottaker(
                mottakertype = Mottakertype.BRUKER,
                navn = "Navn Navnesen",
                adresselinje1 = "Adresseveien 1",
                adresselinje2 = null,
                landkode = "NO",
                postnummer = "0010",
                poststed = "Oslo",
            )

            // Assert
            assertThat(nyBrevmottaker.mottakertype).isEqualTo(Mottakertype.BRUKER)
            assertThat(nyBrevmottaker.navn).isEqualTo("Navn Navnesen")
            assertThat(nyBrevmottaker.adresselinje1).isEqualTo("Adresseveien 1")
            assertThat(nyBrevmottaker.adresselinje2).isNull()
            assertThat(nyBrevmottaker.postnummer).isEqualTo("0010")
            assertThat(nyBrevmottaker.poststed).isEqualTo("Oslo")
            assertThat(nyBrevmottaker.landkode).isEqualTo("NO")
        }
    }
}