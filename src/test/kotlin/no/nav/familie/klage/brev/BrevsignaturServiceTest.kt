package no.nav.familie.klage.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.oppgave.OppgaveClient
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

internal class BrevsignaturServiceTest {
    private val oppgaveClient = mockk<OppgaveClient>()
    val brevsignaturService = BrevsignaturService(mockk<FeatureToggleService>(relaxed = true), oppgaveClient)

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
        every { oppgaveClient.hentSaksbehandlerInfo("Julenissen") } returns saksbehandler()

        val signaturMedEnhet =
            testWithBrukerContext(preferredUsername = "Julenissen") {
                brevsignaturService.lagSignatur(personopplysningerDto, Fagsystem.EF)
            }

        assertThat(signaturMedEnhet.enhet).isEqualTo("Nav arbeid og ytelser Skien")
        assertThat(signaturMedEnhet.navn).isEqualTo("Julenissen")
    }

    @ParameterizedTest
    @EnumSource(Fagsystem::class)
    fun `skal sette riktig enhet i brevsignatur basert på fagsystem`(fagsystem: Fagsystem) {
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.adressebeskyttelse } returns null
        every { oppgaveClient.hentSaksbehandlerInfo(any()) } returns saksbehandler(enhetsnavn = "NAV ARBEID OG YTELSER SKIEN")

        val signatur =
            testWithBrukerContext {
                brevsignaturService.lagSignatur(personopplysningerDto, fagsystem)
            }

        if (fagsystem == Fagsystem.EF) {
            assertThat(signatur.enhet).isEqualTo("Nav arbeid og ytelser Skien")
        } else {
            assertThat(signatur.enhet).isEqualTo(BrevsignaturService.ENHET_NFP)
        }
    }

    @ParameterizedTest
    @EnumSource(Fagsystem::class)
    fun `skal sette nay signatur uten geografisk lokasjon når enhetsnavn er uvanlig`(fagsystem: Fagsystem) {
        val personopplysningerDto = mockk<PersonopplysningerDto>()

        every { oppgaveClient.hentSaksbehandlerInfo(any()) } returns saksbehandler(enhetsnavn = "NAV ARBEID OG YTELSER UVENTET ENHET")
        every { personopplysningerDto.adressebeskyttelse } returns null

        val signatur =
            testWithBrukerContext {
                brevsignaturService.lagSignatur(personopplysningerDto, fagsystem)
            }

        if (fagsystem == Fagsystem.EF) {
            assertThat(signatur.enhet).isEqualTo("Nav arbeid og ytelser")
        } else {
            assertThat(signatur.enhet).isEqualTo(BrevsignaturService.ENHET_NFP)
        }
    }

    @ValueSource(
        strings = ["NAV ARBEID OG YTELSER SKIEN", "NAV ARBEID OG YTELSER MØRE OG ROMSDAL", "NAV ARBEID OG YTELSER SØRLANDET"],
    )
    @ParameterizedTest
    fun `skal sette vanlig nay signatur med geografisk lokasjon`(enhetsnavn: String) {
        val personopplysningerDto = mockk<PersonopplysningerDto>()

        every { oppgaveClient.hentSaksbehandlerInfo(any()) } returns saksbehandler(enhetsnavn = enhetsnavn)
        every { personopplysningerDto.adressebeskyttelse } returns null

        val signaturEF =
            testWithBrukerContext {
                brevsignaturService.lagSignatur(personopplysningerDto, Fagsystem.EF)
            }
        val signaturBA =
            testWithBrukerContext {
                brevsignaturService.lagSignatur(personopplysningerDto, Fagsystem.BA)
            }
        val signaturKS =
            testWithBrukerContext {
                brevsignaturService.lagSignatur(personopplysningerDto, Fagsystem.KS)
            }

        assertThat(signaturEF.enhet).isEqualTo(enhetsnavnTilVisningstekst[enhetsnavn])
        assertThat(signaturBA.enhet).isEqualTo(BrevsignaturService.ENHET_NFP)
        assertThat(signaturKS.enhet).isEqualTo(BrevsignaturService.ENHET_NFP)
    }

    companion object {
        val enhetsnavnTilVisningstekst =
            mapOf(
                "NAV ARBEID OG YTELSER SKIEN" to "Nav arbeid og ytelser Skien",
                "NAV ARBEID OG YTELSER MØRE OG ROMSDAL" to "Nav arbeid og ytelser Møre og Romsdal",
                "NAV ARBEID OG YTELSER SØRLANDET" to "Nav arbeid og ytelser Sørlandet",
            )
    }

    fun saksbehandler(enhetsnavn: String = "NAV ARBEID OG YTELSER SKIEN") =
        Saksbehandler(
            azureId = UUID.randomUUID(),
            navIdent = "NAV123",
            fornavn = "Darth",
            etternavn = "Vader",
            enhet = "4489",
            enhetsnavn = enhetsnavn,
        )
}
