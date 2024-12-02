package no.nav.familie.klage.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class BrevsignaturServiceTest {
    val brevsignaturService = BrevsignaturService(mockk<FeatureToggleService>(relaxed = true))

    @Test
    fun `skal anonymisere signatur hvis strengt fortrolig`() {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns Adressebeskyttelse.STRENGT_FORTROLIG

        val signaturMedEnhet = brevsignaturService.lagSignatur(personopplysningerDto, Fagsystem.EF)

        assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_VIKAFOSSEN)
        assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
    }

    @Test
    internal fun `skal returnere saksehandlers navn og enhet hvis ikke strengt fortrolig`() {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns null

        val signaturMedEnhet = testWithBrukerContext(preferredUsername = "Julenissen") {
            brevsignaturService.lagSignatur(personopplysningerDto, Fagsystem.EF)
        }

        assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        assertThat(signaturMedEnhet.navn).isEqualTo("Julenissen")
    }

    @ParameterizedTest
    @EnumSource(Fagsystem::class)
    fun `skal sette riktig enhet i brevsignatur basert på fagsystem`(fagsystem: Fagsystem) {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns null

        val signaturForEnsligForsørger =
            testWithBrukerContext {
                brevsignaturService.lagSignatur(personopplysningerDto, fagsystem)
            }

        if (fagsystem == Fagsystem.EF) {
            assertThat(signaturForEnsligForsørger.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        } else {
            assertThat(signaturForEnsligForsørger.enhet).isEqualTo(BrevsignaturService.ENHET_NFP)
        }
    }
}
