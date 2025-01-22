package no.nav.familie.klage.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.klage.behandling.dto.OppgaveDto
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.oppgave.TilordnetRessursService
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class BehandlingPåVentServiceTest {
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val behandlinghistorikkService = mockk<BehandlingshistorikkService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val tilordnetRessursService = mockk<TilordnetRessursService>(relaxed = true)

    private val fagsakEf = fagsak()
    private val behandling = behandling(fagsakEf)
    private val behandlingId = behandling.id

    private val behandlingPåVentService = BehandlingPåVentService(
        behandlingService = behandlingService,
        oppgaveService = oppgaveService,
        behandlinghistorikkService = behandlinghistorikkService,
        taskService = taskService,
        tilordnetRessursService = tilordnetRessursService,
    )

    @BeforeEach
    internal fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "Ny saksbehandler"
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class TaAvVent {
        @BeforeEach
        internal fun setUp() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT)
        }

        @Test
        internal fun `sett ny saksbehandler oppgave når behandling tas av vent`() {
            val oppgaveId = 1234567L
            mockSettSaksbehandlerPåOppgave(oppgaveId)

            behandlingPåVentService.taAvVent(behandlingId)

            verify {
                behandlingService.oppdaterStatusPåBehandling(
                    behandlingId = behandlingId,
                    status = BehandlingStatus.UTREDES,
                )
            }
            verify {
                oppgaveService.fordelOppgave(
                    gsakOppgaveId = oppgaveId,
                    saksbehandler = "Ny saksbehandler",
                    versjon = any(),
                )
            }
        }
    }

    private fun mockHentBehandling(
        behandlingStatus: BehandlingStatus,
    ) {
        every { behandlingService.hentBehandling(behandlingId = behandlingId) } returns behandling.copy(status = behandlingStatus)
    }

    private fun mockSettSaksbehandlerPåOppgave(oppgaveId: Long) {
        val oppgave = oppgave(oppgaveId)
        every { tilordnetRessursService.hentOppgave(behandlingId) } returns OppgaveDto(
            oppgaveId = oppgave.id,
            tildeltEnhetsnr = oppgave.tildeltEnhetsnr,
            beskrivelse = oppgave.beskrivelse,
            tilordnetRessurs = oppgave.tilordnetRessurs ?: "",
            prioritet = oppgave.prioritet,
            fristFerdigstillelse = oppgave.fristFerdigstillelse ?: "",
            mappeId = oppgave.mappeId,
            versjon = oppgave.versjon,
        )
        every { oppgaveService.fordelOppgave(any(), any(), any()) } returns oppgaveId
    }

    private fun oppgave(oppgaveId: Long) =
        Oppgave(
            id = oppgaveId,
            tildeltEnhetsnr = "4489",
            tilordnetRessurs = "Saksbehandler",
            beskrivelse = "Beskrivelse",
            mappeId = 101,
            fristFerdigstillelse = LocalDate.of(2025, Month.JANUARY, 1).toString(),
            prioritet = OppgavePrioritet.NORM,
        )
}
