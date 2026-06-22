package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.PdlTestdataHelper.lagNavn
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
            val gjeldendeNavn = lagNavn(fornavn = "Aktiv", historisk = false)
            val historiskNavn = lagNavn(fornavn = "Gammelt", historisk = true)
            val alleNavn = listOf(gjeldendeNavn, historiskNavn)

            // Act
            val resultat = alleNavn.gjeldende()

            // Assert
            assertThat(resultat).isEqualTo(gjeldendeNavn)
        }

        @Test
        fun `skal foretrekke PDL fremfor FREG når begge er gjeldende`() {
            // Arrange
            val pdlNavn = lagNavn(fornavn = "FraPdl", master = "PDL", historisk = false)
            val fregNavn = lagNavn(fornavn = "FraFreg", master = "FREG", historisk = false)
            val alleNavn = listOf(fregNavn, pdlNavn)

            // Act
            val resultat = alleNavn.gjeldende()

            // Assert
            assertThat(resultat).isEqualTo(pdlNavn)
        }

        @Test
        fun `skal foretrekke FREG fremfor andre kilder når PDL mangler`() {
            // Arrange
            val fregNavn = lagNavn(fornavn = "FraFreg", master = "FREG", historisk = false)
            val annenNavn = lagNavn(fornavn = "FraAnnen", master = "ANNEN_KILDE", historisk = false)
            val alleNavn = listOf(annenNavn, fregNavn)

            // Act
            val resultat = alleNavn.gjeldende()

            // Assert
            assertThat(resultat).isEqualTo(fregNavn)
        }

        @Test
        fun `skal bruke navn fra ukjent kilde når det er eneste gjeldende`() {
            // Arrange
            val annenNavn = lagNavn(fornavn = "FraAnnen", master = "ANNEN_KILDE", historisk = false)
            val alleNavn = listOf(annenNavn)

            // Act
            val resultat = alleNavn.gjeldende()

            // Assert
            assertThat(resultat).isEqualTo(annenNavn)
        }

        @Test
        fun `skal behandle master uavhengig av store og små bokstaver`() {
            // Arrange
            val pdlNavn = lagNavn(fornavn = "FraPdl", master = "pdl", historisk = false)
            val fregNavn = lagNavn(fornavn = "FraFreg", master = "freg", historisk = false)
            val alleNavn = listOf(fregNavn, pdlNavn)

            // Act
            val resultat = alleNavn.gjeldende()

            // Assert
            assertThat(resultat).isEqualTo(pdlNavn)
        }

        @Test
        fun `skal ignorere historisk PDL-navn og velge gjeldende FREG-navn`() {
            // Arrange
            val historiskPdlNavn = lagNavn(fornavn = "GammeltPdl", master = "PDL", historisk = true)
            val fregNavn = lagNavn(fornavn = "FraFreg", master = "FREG", historisk = false)
            val alleNavn = listOf(historiskPdlNavn, fregNavn)

            // Act
            val resultat = alleNavn.gjeldende()

            // Assert
            assertThat(resultat).isEqualTo(fregNavn)
        }

        @Test
        fun `skal kaste feil hvis ingen navn finnes`() {
            // Arrange
            val alleNavn = emptyList<Navn>()

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    alleNavn.gjeldende()
                }
            assertThat(exception.message)
                .isEqualTo("Fant ingen gjeldende navn for personen. Forventet minst ett.")
        }

        @Test
        fun `skal kaste feil hvis kun historiske navn finnes`() {
            // Arrange
            val alleNavn =
                listOf(
                    lagNavn(fornavn = "Gammelt1", historisk = true),
                    lagNavn(fornavn = "Gammelt2", historisk = true),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    alleNavn.gjeldende()
                }
            assertThat(exception.message)
                .isEqualTo("Fant ingen gjeldende navn for personen. Forventet minst ett.")
        }

        @Test
        fun `skal kaste feil hvis flere gjeldende navn med samme master finnes`() {
            // Arrange
            val pdlNavn1 = lagNavn(fornavn = "Pdl1", master = "PDL", historisk = false)
            val pdlNavn2 = lagNavn(fornavn = "Pdl2", master = "PDL", historisk = false)
            val alleNavn = listOf(pdlNavn1, pdlNavn2)

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    alleNavn.gjeldende()
                }
            assertThat(exception.message)
                .isEqualTo("Fant flere (2) gjeldende navn for personen med master PDL. Forventet nøyaktig ett.")
        }
    }
}
