package no.nav.familie.klage.institusjon

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InstitusjonRepositoryTest(
    @Autowired private val institusjonRepository: InstitusjonRepository,
) : OppslagSpringRunnerTest() {
    @Nested
    inner class FinnInstitusjon {
        @AfterEach
        fun tearDown() {
            institusjonRepository.deleteAll()
        }

        @Test
        fun `skal lagre institusjon`() {
            // Arrange
            val institusjon = Institusjon(orgNummer = "123456789", navn = "navn", tssEksternId = "tssEksternId")

            // Act
            val lagretInstitusjon = institusjonRepository.insert(institusjon)

            // Assert
            assertThat(lagretInstitusjon.id).isNotNull()
            assertThat(lagretInstitusjon.orgNummer).isEqualTo(institusjon.orgNummer)
            assertThat(lagretInstitusjon.navn).isEqualTo(institusjon.navn)
            assertThat(lagretInstitusjon.tssEksternId).isEqualTo(institusjon.tssEksternId)
        }

        @Test
        fun `skal finne institusjon da den allerde finnes`() {
            // Arrange
            val institusjon = Institusjon(orgNummer = "123456789", navn = "navn", tssEksternId = "tssEksternId")

            institusjonRepository.insert(institusjon)

            // Act
            val lagretInstitusjon = institusjonRepository.finnInstitusjon(institusjon.orgNummer)

            // Assert
            assertThat(lagretInstitusjon).isEqualTo(institusjon)
        }

        @Test
        fun `skal ikke finne institusjon om den ikke finnes`() {
            // Arrange
            val orgNummer = "123456789"

            // Act
            val lagretInstitusjon = institusjonRepository.finnInstitusjon(orgNummer)

            // Assert
            assertThat(lagretInstitusjon).isNull()
        }
    }
}