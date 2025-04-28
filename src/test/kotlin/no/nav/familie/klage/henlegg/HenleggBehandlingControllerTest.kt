package no.nav.familie.klage.henlegg

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Test
import java.util.UUID

class HenleggBehandlingControllerTest {
    val taskService = mockk<TaskService>(relaxed = true)
    val tilgangService = mockk<TilgangService>(relaxed = true)
    val henleggBehandlingService = mockk<HenleggBehandlingService>(relaxed = true)
    val brevService = mockk<BrevService>(relaxed = true)

    val henleggBehandlingController =
        HenleggBehandlingController(
            tilgangService = tilgangService,
            henleggBehandlingService = henleggBehandlingService,
            brevService = brevService,
        )

    @Test
    internal fun `Skal lage send brev task hvis send brev er true og henlagårsak er trukket`() {
        brevService.opprettJournalførHenleggelsesbrevTask(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
        verify(exactly = 1) { brevService.opprettJournalførHenleggelsesbrevTask(any(), any()) }
    }

    @Test
    internal fun `Skal ikke lage send brev task hvis skalSendeHenleggelsesBrev er false`() {
        henleggBehandlingController.henleggBehandling(
            UUID.randomUUID(),
            HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, false),
        )
        verify(exactly = 0) { brevService.opprettJournalførHenleggelsesbrevTask(any(), any()) }
    }
}
