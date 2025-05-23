package no.nav.familie.klage.oppgave

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata.KLAGE_GJELDER_TILBAKEKREBING_METADATA_KEY
import no.nav.familie.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Properties
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class OppgaveTaskServiceTest {
    val oppgaveClient = mockk<OppgaveClient>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()

    val opprettBehandleSakOppgaveTask =
        OpprettBehandleSakOppgaveTask(
            fagsakService = fagsakService,
            oppgaveClient = oppgaveClient,
            behandlingService = behandlingService,
            behandleSakOppgaveRepository = behandleSakOppgaveRepository,
        )

    val fagsak = DomainUtil.fagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak)

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext()
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class OpprettBehandleSakOppgave {
        lateinit var oppgaveSlot: CapturingSlot<OpprettOppgaveRequest>
        val oppgaveId = 1L

        @BeforeEach
        fun setUp() {
            oppgaveSlot = slot()
            every { oppgaveClient.opprettOppgave(capture(oppgaveSlot)) } returns oppgaveId
            every { behandleSakOppgaveRepository.insert(any()) } answers { firstArg() }
        }

        @Test
        internal fun `skal opprette behandleSak oppgave med riktige verdier for ny klagebehandling`() {
            val behandleSakOppgaveTask =
                Task(
                    type = OpprettBehandleSakOppgaveTask.TYPE,
                    payload = behandling.id.toString(),
                    properties =
                        Properties().apply {
                            this[SAKSBEHANDLER_METADATA_KEY] = ""
                        },
                )

            opprettBehandleSakOppgaveTask.doTask(behandleSakOppgaveTask)

            assertThat(oppgaveSlot.captured.behandlingstema).isNull()
            assertThat(oppgaveSlot.captured.behandlingstype).isEqualTo("ae0058")
            assertThat(oppgaveSlot.captured.behandlesAvApplikasjon).isEqualTo("familie-klage")
            assertThat(oppgaveSlot.captured.oppgavetype).isEqualTo(Oppgavetype.BehandleSak)
            assertThat(oppgaveSlot.captured.enhetsnummer).isEqualTo("4489")
            assertThat(oppgaveSlot.captured.fristFerdigstillelse).isAfter(LocalDate.now())
            assertThat(oppgaveSlot.captured.saksId).isEqualTo(fagsak.eksternId)
            assertThat(oppgaveSlot.captured.tema).isEqualTo(Tema.ENF)
            assertThat(oppgaveSlot.captured.tilordnetRessurs).isNotNull
        }

        @Test
        internal fun `skal opprette behandleSakOppgave med behandlingstema klage tilbakekreving`() {
            val behandleSakOppgaveTask =
                Task(
                    type = OpprettBehandleSakOppgaveTask.TYPE,
                    payload = behandling.id.toString(),
                    properties =
                        Properties().apply {
                            this[KLAGE_GJELDER_TILBAKEKREBING_METADATA_KEY] = true.toString()
                        },
                )

            opprettBehandleSakOppgaveTask.doTask(behandleSakOppgaveTask)

            assertThat(oppgaveSlot.captured.behandlingstema).isEqualTo("ab0007")
        }
    }

    @Test
    internal fun `skal opprette en behandleSakOppgave i databasen`() {
        val oppgaveId = Random.nextLong().absoluteValue
        val behandleSakOppgaveSlot = slot<BehandleSakOppgave>()
        every { oppgaveClient.opprettOppgave(any()) } returns oppgaveId
        every { behandleSakOppgaveRepository.insert(capture(behandleSakOppgaveSlot)) } answers { firstArg() }
        val behandleSakOppgaveTask =
            Task(
                type = OpprettBehandleSakOppgaveTask.TYPE,
                payload = behandling.id.toString(),
            )

        opprettBehandleSakOppgaveTask.doTask(behandleSakOppgaveTask)

        assertThat(behandleSakOppgaveSlot.captured.oppgaveId).isEqualTo(oppgaveId)
        assertThat(behandleSakOppgaveSlot.captured.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    internal fun `skal forsøke å opprette behandleSakOppgave uten tilordnet ressurs ved tilgangsfeil`() {
        val behandleSakOppgaveSlot = slot<BehandleSakOppgave>()
        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()

        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } throws
            IllegalStateException("En uventet feil oppstod: Mottatt navIdent har ikke tilgang til enheten")
        every { behandleSakOppgaveRepository.insert(capture(behandleSakOppgaveSlot)) } answers { firstArg() }
        val behandleSakOppgaveTask =
            Task(
                type = OpprettBehandleSakOppgaveTask.TYPE,
                payload = behandling.id.toString(),
            )

        val exception = assertThrows<IllegalStateException> { opprettBehandleSakOppgaveTask.doTask(behandleSakOppgaveTask) }

        assertThat(exception.message).contains("navIdent har ikke tilgang til enheten")
        verify(exactly = 2) { oppgaveClient.opprettOppgave(any()) }
        assertThat(oppgaveRequestSlot.captured.tilordnetRessurs).isNull()
    }
}
