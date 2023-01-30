package no.nav.familie.klage.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BrevsignaturServiceTest {

    val brevsignaturService = BrevsignaturService()

    @Test
    fun `skal anonymisere signatur hvis strengt fortrolig`() {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns Adressebeskyttelse.STRENGT_FORTROLIG

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysningerDto)

        assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_VIKAFOSSEN)
        assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
    }

    @Test
    internal fun `skal returnere saksehandlers navn og enhet hvis ikke strengt fortrolig`() {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns null

        val signaturMedEnhet = testWithBrukerContext(preferredUsername = "Julenissen") {
            brevsignaturService.lagSignatur(personopplysningerDto)
        }

        assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        assertThat(signaturMedEnhet.navn).isEqualTo("Julenissen")
    }
}
