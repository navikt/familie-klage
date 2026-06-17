package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlPersonUtilTest {
    @Nested
    inner class Gjeldende {
        @Test
        fun `skal finne gjeldende navn og filtrere bort historiske navn`() {
            // Arrange
            val navn1 =
                Navn(
                    fornavn = "Fornavn1",
                    mellomnavn = "Mellomnavn1",
                    etternavn = "Etternavn1",
                    metadata = Metadata(historisk = false),
                )

            val navn2 =
                Navn(
                    fornavn = "Fornavn2",
                    mellomnavn = "Mellomnavn2",
                    etternavn = "Etternavn2",
                    metadata = Metadata(historisk = true),
                )

            val alleNavn = listOf(navn1, navn2)

            // Act
            val gjeldendeNavn = alleNavn.gjeldende()

            assertThat(gjeldendeNavn).isEqualTo(navn1)
        }

        @Test
        fun `skal kaste feil hvis ingen navn finnnes`() {
            // Arrange
            val alleNavn = emptyList<Navn>()

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    alleNavn.gjeldende()
                }
            assertThat(exception.message).isEqualTo("Fant ingen gjeldende navn for personen. Forventet minst ett.")
        }

        @Test
        fun `skal kaste feil hvis flere gjeldende navn finnes`() {
            // Arrange
            val navn1 =
                Navn(
                    fornavn = "Fornavn1",
                    mellomnavn = "Mellomnavn1",
                    etternavn = "Etternavn1",
                    metadata = Metadata(historisk = false),
                )

            val navn2 =
                Navn(
                    fornavn = "Fornavn2",
                    mellomnavn = "Mellomnavn2",
                    etternavn = "Etternavn2",
                    metadata = Metadata(historisk = false),
                )

            val alleNavn = listOf(navn1, navn2)

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    alleNavn.gjeldende()
                }
            assertThat(exception.message).isEqualTo("Fant flere (2) gjeldende navn for personen. Forventet kun ett.")
        }
    }
}
