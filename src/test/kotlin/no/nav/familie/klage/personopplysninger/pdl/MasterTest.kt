package no.nav.familie.klage.personopplysninger.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MasterTest {
    @Nested
    inner class FraVerdi {
        @Test
        fun `skal mappe til PDL enum for store bokstaver`() {
            // Act
            val master = Master.fraVerdi("PDL")

            // Assert
            assertThat(master).isEqualTo(Master.PDL)
        }

        @Test
        fun `skal mappe til PDL enum for små bokstaver`() {
            // Act
            val master = Master.fraVerdi("pdl")

            // Assert
            assertThat(master).isEqualTo(Master.PDL)
        }

        @Test
        fun `skal mappe til PDL enum for store og små bokstaver`() {
            // Act
            val master = Master.fraVerdi("pDl")

            // Assert
            assertThat(master).isEqualTo(Master.PDL)
        }

        @Test
        fun `skal mappe til FREG enum for store bokstaver`() {
            // Act
            val master = Master.fraVerdi("FREG")

            // Assert
            assertThat(master).isEqualTo(Master.FREG)
        }

        @Test
        fun `skal mappe til FREG enum for små bokstaver`() {
            // Act
            val master = Master.fraVerdi("freg")

            // Assert
            assertThat(master).isEqualTo(Master.FREG)
        }

        @Test
        fun `skal mappe til FREG enum for store og små bokstaver`() {
            // Act
            val master = Master.fraVerdi("fReG")

            // Assert
            assertThat(master).isEqualTo(Master.FREG)
        }

        @Test
        fun `skal mappe til UKJENT enum for store bokstaver`() {
            // Act
            val master = Master.fraVerdi("UKJENT")

            // Assert
            assertThat(master).isEqualTo(Master.UKJENT)
        }

        @Test
        fun `skal mappe til UKJENT enum for små bokstaver`() {
            // Act
            val master = Master.fraVerdi("ukjent")

            // Assert
            assertThat(master).isEqualTo(Master.UKJENT)
        }

        @Test
        fun `skal mappe til UKJENT enum for store og små bokstaver`() {
            // Act
            val master = Master.fraVerdi("uKjEnT")

            // Assert
            assertThat(master).isEqualTo(Master.UKJENT)
        }
    }
}
