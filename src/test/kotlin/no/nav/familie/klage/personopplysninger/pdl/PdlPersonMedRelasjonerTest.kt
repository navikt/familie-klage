package no.nav.familie.klage.personopplysninger.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdlPersonMedRelasjonerTest {
    @Test
    fun `skal være barn når relatert person har rollen BARN`() {
        // Arrange
        val relasjon = PdlForelderBarnRelasjon(relatertPersonsIdent = PERSONIDENT, relatertPersonsRolle = "BARN")

        // Act & Assert
        assertThat(relasjon.erBarn()).isTrue
    }

    @Test
    fun `skal ikke være barn når relatert person har en foreldrerolle`() {
        // Arrange
        val relasjon = PdlForelderBarnRelasjon(relatertPersonsIdent = PERSONIDENT, relatertPersonsRolle = "MOR")

        // Act & Assert
        assertThat(relasjon.erBarn()).isFalse
    }

    @Test
    fun `skal maskere identene i toString`() {
        // Arrange
        val person =
            PdlPersonMedRelasjoner(
                forelderBarnRelasjon = listOf(PdlForelderBarnRelasjon(relatertPersonsIdent = PERSONIDENT, relatertPersonsRolle = "BARN")),
                sivilstand = listOf(PdlSivilstand(relatertVedSivilstand = PERSONIDENT_2)),
            )

        // Act
        val tekst = person.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT, PERSONIDENT_2)
        assertThat(tekst).contains("relatertPersonsRolle=BARN")
    }

    companion object {
        private const val PERSONIDENT = "12345678910"
        private const val PERSONIDENT_2 = "10987654321"
    }
}
