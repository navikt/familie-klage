package no.nav.familie.klage.infrastruktur.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiFeilTest {
    @Nested
    inner class FabrikkTest {
        @Test
        fun `skal opprette bad request`() {
            // Act
            val apiFeil = ApiFeil.badRequest("min feilmelding")

            // Assert
            assertThat(apiFeil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(apiFeil.feilmelding).isEqualTo("min feilmelding")
            assertThat(apiFeil.message).isEqualTo("min feilmelding")
        }
    }
}
