package no.nav.familie.klage.institusjon

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class InstitusjonRepositoryTest(
    @Autowired private val institusjonRepository: InstitusjonRepository,
) : OppslagSpringRunnerTest() {
    @AfterEach
    fun tearDown() {
        institusjonRepository.deleteAll()
    }

    @Nested
    inner class Insert {
        @Test
        fun `skal lagre institusjon`() {
            // Arrange
            val institusjon = Institusjon(orgNummer = "123456789", navn = "navn")

            // Act
            val lagretInstitusjon = institusjonRepository.insert(institusjon)

            // Assert
            assertThat(lagretInstitusjon.id).isNotNull()
            assertThat(lagretInstitusjon.orgNummer).isEqualTo(institusjon.orgNummer)
            assertThat(lagretInstitusjon.navn).isEqualTo(institusjon.navn)
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `skal finne institusjon ved å bruke id`() {
            // Arrange
            val institusjon =
                Institusjon(
                    id = UUID.randomUUID(),
                    orgNummer = "123456789",
                    navn = "navn",
                )

            institusjonRepository.insert(institusjon)

            // Act
            val funnetInstitusjon = institusjonRepository.findById(institusjon.id)

            // Assert
            assertThat(funnetInstitusjon).isPresent()
            assertThat(funnetInstitusjon.get()).isEqualTo(institusjon)
        }
    }

    @Nested
    inner class FinnInstitusjon {
        @Test
        fun `skal finne institusjon med orgnummer da den allerde finnes`() {
            // Arrange
            val institusjon = Institusjon(orgNummer = "123456789", navn = "navn")

            institusjonRepository.insert(institusjon)

            // Act
            val lagretInstitusjon = institusjonRepository.finnInstitusjon(institusjon.orgNummer)

            // Assert
            assertThat(lagretInstitusjon).isEqualTo(institusjon)
        }

        @Test
        fun `skal ikke finne institusjon med orgnummer om den ikke finnes`() {
            // Arrange
            val orgNummer = "123456789"

            // Act
            val lagretInstitusjon = institusjonRepository.finnInstitusjon(orgNummer)

            // Assert
            assertThat(lagretInstitusjon).isNull()
        }
    }
}
