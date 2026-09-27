package no.nav.familie.klage.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.pdl.PdlClient
import no.nav.familie.klage.personopplysninger.pdl.PdlForelderBarnRelasjon
import no.nav.familie.klage.personopplysninger.pdl.PdlPersonMedRelasjoner
import no.nav.familie.klage.personopplysninger.pdl.PdlSivilstand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PersonMedRelasjonerServiceTest {
    private val pdlClient = mockk<PdlClient>()
    private val personMedRelasjonerService = PersonMedRelasjonerService(pdlClient)

    @Test
    fun `skal returnere personen, barna, personer relatert via sivilstand og barnas andre foreldre`() {
        // Arrange
        every { pdlClient.hentPersonMedRelasjoner(SØKER) } returns
            personMedRelasjoner(
                forelderBarnRelasjon = listOf(barnRelasjon(BARN_1), barnRelasjon(BARN_2), forelderRelasjon(SØKERS_MOR, "MOR")),
                sivilstand = listOf(PdlSivilstand(EKTEFELLE), PdlSivilstand(relatertVedSivilstand = null)),
            )
        every { pdlClient.hentPersonerMedRelasjoner(listOf(BARN_1, BARN_2)) } returns
            mapOf(
                BARN_1 to personMedRelasjoner(forelderBarnRelasjon = listOf(forelderRelasjon(SØKER, "MOR"), forelderRelasjon(ANNEN_FORELDER, "FAR"))),
                BARN_2 to personMedRelasjoner(forelderBarnRelasjon = listOf(forelderRelasjon(SØKER, "MOR"), forelderRelasjon(relatertPersonsIdent = null, rolle = "FAR"))),
            )

        // Act
        val identer = personMedRelasjonerService.hentIdenterForPersonMedRelasjoner(SØKER)

        // Assert
        assertThat(identer).containsExactlyInAnyOrder(SØKER, BARN_1, BARN_2, EKTEFELLE, ANNEN_FORELDER)
    }

    @Test
    fun `skal bare slå opp personen når personen ikke har barn`() {
        // Arrange
        every { pdlClient.hentPersonMedRelasjoner(SØKER) } returns personMedRelasjoner(sivilstand = listOf(PdlSivilstand(EKTEFELLE)))

        // Act
        val identer = personMedRelasjonerService.hentIdenterForPersonMedRelasjoner(SØKER)

        // Assert
        assertThat(identer).containsExactlyInAnyOrder(SØKER, EKTEFELLE)
        verify(exactly = 0) { pdlClient.hentPersonerMedRelasjoner(any()) }
    }

    @Test
    fun `skal kaste feil når PDL ikke svarer for alle barna`() {
        // Arrange
        every { pdlClient.hentPersonMedRelasjoner(SØKER) } returns
            personMedRelasjoner(forelderBarnRelasjon = listOf(barnRelasjon(BARN_1), barnRelasjon(BARN_2)))
        every { pdlClient.hentPersonerMedRelasjoner(listOf(BARN_1, BARN_2)) } returns
            mapOf(BARN_1 to personMedRelasjoner(forelderBarnRelasjon = listOf(forelderRelasjon(SØKER, "MOR"))))

        // Act
        val feil = assertThrows<Feil> { personMedRelasjonerService.hentIdenterForPersonMedRelasjoner(SØKER) }

        // Assert
        assertThat(feil.message).isEqualTo("Fikk ikke svar fra PDL for 1 av 2 barn ved henting av barnas foreldre.")
    }

    private fun personMedRelasjoner(
        forelderBarnRelasjon: List<PdlForelderBarnRelasjon> = emptyList(),
        sivilstand: List<PdlSivilstand> = emptyList(),
    ) = PdlPersonMedRelasjoner(forelderBarnRelasjon = forelderBarnRelasjon, sivilstand = sivilstand)

    private fun barnRelasjon(barn: String) = PdlForelderBarnRelasjon(relatertPersonsIdent = barn, relatertPersonsRolle = "BARN")

    private fun forelderRelasjon(
        relatertPersonsIdent: String?,
        rolle: String,
    ) = PdlForelderBarnRelasjon(relatertPersonsIdent = relatertPersonsIdent, relatertPersonsRolle = rolle)

    companion object {
        private const val SØKER = "01010112345"
        private const val BARN_1 = "01011512345"
        private const val BARN_2 = "01011812345"
        private const val EKTEFELLE = "02020212345"
        private const val ANNEN_FORELDER = "03030312345"
        private const val SØKERS_MOR = "04040412345"
    }
}
