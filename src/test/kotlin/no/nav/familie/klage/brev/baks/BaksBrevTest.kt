package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.felles.domain.Fil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class BaksBrevTest {
    @Nested
    inner class PdfSomBytesTest {
        @Test
        fun `skal konvertere pdf til bytes`() {
            // Arrange
            val bytes = "data".toByteArray()

            val baksBrev = BaksBrev(
                UUID.randomUUID(),
                "<div/>",
                Fil(bytes),
            )

            // Act
            val pdfSomBytes = baksBrev.pdfSomBytes()

            // Assert
            assertThat(pdfSomBytes).isEqualTo(bytes)
        }
    }
}
