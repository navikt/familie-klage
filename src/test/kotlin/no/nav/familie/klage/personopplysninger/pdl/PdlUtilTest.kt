package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.klage.infrastruktur.exception.PdlRequestException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlUtilTest {
    @Test
    fun `skal returnere personen fra bolkoppslaget`() {
        // Arrange
        val person = PdlPersonMedRelasjoner(forelderBarnRelasjon = emptyList(), sivilstand = emptyList())
        val pdlResponse = bolkResponse(PersonDataBolk(ident = PERSONIDENT, code = "ok", person = person))

        // Act
        val resultat = feilsjekkOgReturnerPersonFraBolk(PERSONIDENT, pdlResponse)

        // Assert
        assertThat(resultat).isEqualTo(person)
    }

    @Test
    fun `skal kaste PdlNotFoundException når personen ikke finnes i PDL`() {
        // Arrange
        val pdlResponse = bolkResponse(PersonDataBolk<PdlPersonMedRelasjoner>(ident = PERSONIDENT, code = "not_found", person = null))

        // Act & Assert
        assertThrows<PdlNotFoundException> { feilsjekkOgReturnerPersonFraBolk(PERSONIDENT, pdlResponse) }
    }

    @Test
    fun `skal kaste PdlRequestException når PDL svarer med en annen feilkode`() {
        // Arrange
        val pdlResponse = bolkResponse(PersonDataBolk<PdlPersonMedRelasjoner>(ident = PERSONIDENT, code = "bad_request", person = null))

        // Act
        val feil = assertThrows<PdlRequestException> { feilsjekkOgReturnerPersonFraBolk(PERSONIDENT, pdlResponse) }

        // Assert
        assertThat(feil).isNotInstanceOf(PdlNotFoundException::class.java)
    }

    private fun <T> bolkResponse(vararg personer: PersonDataBolk<T>) = PdlBolkResponse(data = PersonBolk(personBolk = personer.toList()), errors = null, extensions = null)

    companion object {
        private const val PERSONIDENT = "12345678910"
    }
}
