package no.nav.familie.klage.henlegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.dto.FullmaktDto
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.VergemålDto
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class HenleggBehandlingControllerTest {
    val taskService = mockk<TaskService>(relaxed = true)
    val tilgangService = mockk<TilgangService>(relaxed = true)
    val henleggBehandlingService = mockk<HenleggBehandlingService>(relaxed = true)
    val brevService = mockk<BrevService>(relaxed = true)
    val personopplysningerService = mockk<PersonopplysningerService>(relaxed = true)

    val henleggBehandlingController = HenleggBehandlingController(
        tilgangService = tilgangService,
        henleggBehandlingService = henleggBehandlingService,
        brevService = brevService,
        personopplysningerService = personopplysningerService,
    )

    @Test
    internal fun `Skal kaste feil hvis feilregistrert og send brev er true`() {
        val exception =
            org.junit.jupiter.api.assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(
                    UUID.randomUUID(),
                    HenlagtDto(HenlagtÅrsak.FEILREGISTRERT, true),
                )
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis type er ulik trukket tilbake")
    }

    @Test
    internal fun `Skal lage send brev task hvis send brev er true og henlagårsak er trukket`() {
        henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
        verify(exactly = 1) { brevService.opprettJournalførHenleggelsesbrevTask(any()) }
    }

    @Test
    internal fun `Skal ikke lage send brev task hvis skalSendeHenleggelsesBrev er false`() {
        henleggBehandlingController.henleggBehandling(
            UUID.randomUUID(),
            HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, false),
        )
        verify(exactly = 0) { brevService.opprettJournalførHenleggelsesbrevTask(any()) }
    }

    @Test internal fun `Skal kaste feil hvis bruker har fullmakt`() {
        mocckHentPersonopplysningerMedFullmakt()
        val exception =
            assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
    }

    @Test internal fun `Skal ikke kaste feil hvis bruker har fullmakt som har utgått`() {
        mocckHentPersonopplysningerMedFullmaktEnDagSiden()
        henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(årsak = HenlagtÅrsak.TRUKKET_TILBAKE, skalSendeHenleggelsesbrev = true))
        verify(exactly = 1) { brevService.opprettJournalførHenleggelsesbrevTask(any()) }
    }

    @Test internal fun `Skal kaste feil hvis bruker har Verge`() {
        mockkHentPersonopplysningerMedVergemål()
        val exception =
            assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(årsak = HenlagtÅrsak.TRUKKET_TILBAKE, skalSendeHenleggelsesbrev = true))
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
    }

    private fun mocckHentPersonopplysningerMedFullmaktEnDagSiden() {
        every { personopplysningerService.hentPersonopplysninger(any()) } returns
            dto(
                fullmakt =
                listOf(
                    FullmaktDto(
                        gyldigFraOgMed = LocalDate.now().minusDays(2),
                        gyldigTilOgMed = LocalDate.now().minusDays(1),
                        navn = "123",
                        motpartsPersonident = "123",
                        områder = emptyList(),
                    ),
                ),
            )
    }

    private fun mocckHentPersonopplysningerMedFullmakt() {
        every { personopplysningerService.hentPersonopplysninger(any()) } returns
            dto(
                fullmakt =
                listOf(
                    FullmaktDto(
                        gyldigFraOgMed = LocalDate.now(),
                        gyldigTilOgMed = null,
                        navn = "123",
                        motpartsPersonident = "123",
                        områder = emptyList(),
                    ),
                ),
            )
    }

    private fun mockkHentPersonopplysningerMedVergemål() {
        every { personopplysningerService.hentPersonopplysninger(any()) } returns
            dto(
                vergemål =
                listOf(
                    VergemålDto(
                        embete = null,
                        type = null,
                        motpartsPersonident = null,
                        navn = null,
                        omfang = null,
                    ),
                ),
            )
    }

    private fun dto(
        fullmakt: List<FullmaktDto> = emptyList(),
        vergemål: List<VergemålDto> = emptyList(),
    ) = PersonopplysningerDto(
        personIdent = "",
        navn = "",
        kjønn = Kjønn.MANN,
        adressebeskyttelse = null,
        folkeregisterpersonstatus = null,
        dødsdato = null,
        fullmakt = fullmakt,
        egenAnsatt = false,
        vergemål = vergemål,
    )
}
