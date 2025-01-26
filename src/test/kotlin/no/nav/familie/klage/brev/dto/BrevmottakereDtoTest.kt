package no.nav.familie.klage.brev.dto

import no.nav.familie.klage.brevmottaker.dto.BrevmottakereDto
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.testutil.DomainUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.util.UUID

class BrevmottakereDtoTest {
    @Nested
    inner class ValiderTest {
        @Test
        fun `skal kaste exception om både personer og organisasjoner er tom`() {
            // Arrange
            val brevmottakereDto = BrevmottakereDto(
                personer = emptyList(),
                organisasjoner = emptyList(),
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                brevmottakereDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.feilmelding).isEqualTo("Må ha minimum en brevmottaker.")
            assertThat(exception.message).isEqualTo("Må ha minimum en brevmottaker.")
        }

        @Test
        fun `skal kaste exception det er en duplikat ident for person med ident`() {
            // Arrange
            val brevmottakereDto = BrevmottakereDto(
                personer = listOf(
                    DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                    DomainUtil.lagBrevmottakerPersonMedIdent(personIdent = "1"),
                ),
                organisasjoner = emptyList(),
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                brevmottakereDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.feilmelding).isEqualTo("En person kan bare legges til en gang som brevmottaker.")
            assertThat(exception.message).isEqualTo("En person kan bare legges til en gang som brevmottaker.")
        }

        @Test
        fun `skal kaste exception det er en duplikat id for person uten ident`() {
            // Arrange
            val id = UUID.randomUUID()

            val brevmottakereDto = BrevmottakereDto(
                personer = listOf(
                    DomainUtil.lagBrevmottakerPersonUtenIdent(id = id),
                    DomainUtil.lagBrevmottakerPersonUtenIdent(id = id),
                ),
                organisasjoner = emptyList(),
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                brevmottakereDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.feilmelding).isEqualTo("En person kan bare legges til en gang som brevmottaker.")
            assertThat(exception.message).isEqualTo("En person kan bare legges til en gang som brevmottaker.")
        }

        @Test
        fun `skal kaste exception det er et duplikat orgnr for organisasjoner`() {
            // Arrange
            val brevmottakereDto = BrevmottakereDto(
                personer = emptyList(),
                organisasjoner = listOf(
                    DomainUtil.lagBrevmottakerOrganisasjon(organisasjonsnummer = "123"),
                    DomainUtil.lagBrevmottakerOrganisasjon(organisasjonsnummer = "123"),
                ),
            )

            // Act & assert
            val exception = assertThrows<ApiFeil> {
                brevmottakereDto.valider()
            }
            assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(exception.feilmelding).isEqualTo("En organisasjon kan bare legges til en gang som brevmottaker.")
            assertThat(exception.message).isEqualTo("En organisasjon kan bare legges til en gang som brevmottaker.")
        }
    }
}
