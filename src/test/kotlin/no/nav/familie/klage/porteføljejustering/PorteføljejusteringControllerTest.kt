package no.nav.familie.klage.porteføljejustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.enhet.BarnetrygdEnhet
import no.nav.familie.klage.behandling.enhet.KontantstøtteEnhet
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkHendelse
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTaskPayload
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.BehandleSakOppgave
import no.nav.familie.klage.oppgave.BehandleSakOppgaveRepository
import no.nav.familie.klage.porteføljejustering.PorteføljejusteringController.OppdaterBehandlendeEnhetRequest
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class PorteføljejusteringControllerTest {
    private val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val taskService = mockk<TaskService>()

    private val porteføljejusteringController =
        PorteføljejusteringController(
            behandleSakOppgaveRepository = behandleSakOppgaveRepository,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            behandlingshistorikkService = behandlingshistorikkService,
            taskService = taskService,
        )

    private val fagsak = fagsak()
    private val behandlingId = UUID.randomUUID()
    private val oppgaveId = 123L

    @BeforeEach
    fun setup() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) } returns mockk()
        every { taskService.save(any()) } returns mockk()
    }

    @Test
    fun `skal kaste feil hvis BehandleSakOppgave ikke finnes for oppgaveId`() {
        // Arrange
        val request =
            OppdaterBehandlendeEnhetRequest(
                oppgaveId = oppgaveId,
                nyEnhet = BarnetrygdEnhet.OSLO.enhetsnummer,
                fagsystem = Fagsystem.BA,
            )

        every { behandleSakOppgaveRepository.findByOppgaveId(oppgaveId) } returns null

        // Act & Assert
        val exception =
            assertThrows<IllegalStateException> {
                porteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)
            }

        assertThat(exception.message).isEqualTo("Fant ikke BehandleSakOppgave for oppgaveId=$oppgaveId")
    }

    @Test
    fun `skal kaste feil hvis ugyldig enhetsnummer oppgis`() {
        // Arrange
        val behandling =
            DomainUtil.behandling(
                id = behandlingId,
                behandlendeEnhet = BarnetrygdEnhet.OSLO.enhetsnummer,
            )
        val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId)

        val request =
            OppdaterBehandlendeEnhetRequest(
                oppgaveId = oppgaveId,
                nyEnhet = "9999",
                fagsystem = Fagsystem.BA,
            )

        every { behandleSakOppgaveRepository.findByOppgaveId(oppgaveId) } returns behandleSakOppgave
        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        // Act & Assert
        val exception =
            assertThrows<NoSuchElementException> {
                porteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)
            }

        assertThat(exception.message).isEqualTo("Array contains no element matching the predicate.")

        verify(exactly = 0) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
        verify(exactly = 0) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `skal returnere melding om at enhet allerede er satt hvis behandlende enhet er lik ny enhet`() {
        // Arrange
        val behandling =
            DomainUtil.behandling(
                id = behandlingId,
                behandlendeEnhet = BarnetrygdEnhet.OSLO.enhetsnummer,
            )
        val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId)

        val request =
            OppdaterBehandlendeEnhetRequest(
                oppgaveId = oppgaveId,
                nyEnhet = BarnetrygdEnhet.OSLO.enhetsnummer,
                fagsystem = Fagsystem.BA,
            )

        every { behandleSakOppgaveRepository.findByOppgaveId(oppgaveId) } returns behandleSakOppgave
        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        // Act
        val result = porteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)

        // Assert
        assertThat(result.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(result.data).isEqualTo("Behandlende enhet er allerede satt til ${BarnetrygdEnhet.OSLO.enhetsnummer}. Ingen oppdatering gjort.")

        verify(exactly = 0) { behandlingService.oppdaterBehandlendeEnhet(any(), any(), any()) }
        verify(exactly = 0) { behandlingshistorikkService.opprettBehandlingshistorikk(any(), any(), any(), any()) }
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `skal oppdatere behandlende enhet og opprette behandlingshistorikk for BA`() {
        // Arrange
        val gammelEnhet = BarnetrygdEnhet.DRAMMEN
        val nyEnhet = BarnetrygdEnhet.OSLO
        val behandling =
            DomainUtil.behandling(
                id = behandlingId,
                behandlendeEnhet = gammelEnhet.enhetsnummer,
            )
        val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId)

        val request =
            OppdaterBehandlendeEnhetRequest(
                oppgaveId = oppgaveId,
                nyEnhet = nyEnhet.enhetsnummer,
                fagsystem = Fagsystem.BA,
            )

        val taskSlot = slot<Task>()

        every { behandleSakOppgaveRepository.findByOppgaveId(oppgaveId) } returns behandleSakOppgave
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingService.oppdaterBehandlendeEnhet(behandlingId, nyEnhet, Fagsystem.BA) } just runs
        every { taskService.save(capture(taskSlot)) } returns mockk()

        // Act
        val result = porteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)

        // Assert
        val lagretTaskPayload = objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot.captured.payload)

        assertThat(result.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(result.data).isEqualTo("Behandlende enhet oppdatert til ${nyEnhet.enhetsnummer}.")
        assertThat(lagretTaskPayload.behandlingId).isEqualTo(behandlingId)
        assertThat(lagretTaskPayload.gjeldendeSaksbehandler).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(lagretTaskPayload.hendelse).isEqualTo(BehandlingsstatistikkHendelse.PÅBEGYNT)

        verify(exactly = 1) {
            behandlingService.oppdaterBehandlendeEnhet(
                behandlingId = behandlingId,
                behandlendeEnhet = nyEnhet,
                fagsystem = Fagsystem.BA,
            )
        }

        verify(exactly = 1) {
            behandlingshistorikkService.opprettBehandlingshistorikk(
                behandlingId = behandlingId,
                steg = behandling.steg,
                historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET,
                beskrivelse = "Behandlende enhet endret fra NAV Familie- og pensjonsytelser Drammen til NAV Familie- og pensjonsytelser Oslo 1 i forbindelse med porteføljejustering januar 2026.",
            )
        }
    }

    @Test
    fun `skal oppdatere behandlende enhet og opprette behandlingshistorikk for KS`() {
        // Arrange
        val gammelEnhet = KontantstøtteEnhet.DRAMMEN
        val nyEnhet = KontantstøtteEnhet.OSLO
        val behandling =
            DomainUtil.behandling(
                id = behandlingId,
                behandlendeEnhet = gammelEnhet.enhetsnummer,
            )
        val behandleSakOppgave = BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId)

        val request =
            OppdaterBehandlendeEnhetRequest(
                oppgaveId = oppgaveId,
                nyEnhet = nyEnhet.enhetsnummer,
                fagsystem = Fagsystem.KS,
            )

        val taskSlot = slot<Task>()

        every { behandleSakOppgaveRepository.findByOppgaveId(oppgaveId) } returns behandleSakOppgave
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingService.oppdaterBehandlendeEnhet(behandlingId, nyEnhet, Fagsystem.KS) } just runs
        every { taskService.save(capture(taskSlot)) } returns mockk()

        // Act
        val result = porteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)

        // Assert
        val lagretTaskPayload = objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot.captured.payload)

        assertThat(result.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(result.data).isEqualTo("Behandlende enhet oppdatert til ${nyEnhet.enhetsnummer}.")
        assertThat(lagretTaskPayload.behandlingId).isEqualTo(behandlingId)
        assertThat(lagretTaskPayload.gjeldendeSaksbehandler).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(lagretTaskPayload.hendelse).isEqualTo(BehandlingsstatistikkHendelse.PÅBEGYNT)

        verify(exactly = 1) {
            behandlingService.oppdaterBehandlendeEnhet(
                behandlingId = behandlingId,
                behandlendeEnhet = nyEnhet,
                fagsystem = Fagsystem.KS,
            )
        }

        verify(exactly = 1) {
            behandlingshistorikkService.opprettBehandlingshistorikk(
                behandlingId = behandlingId,
                steg = behandling.steg,
                historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET,
                beskrivelse = "Behandlende enhet endret fra NAV Familie- og pensjonsytelser Drammen til NAV Familie- og pensjonsytelser Oslo 1 i forbindelse med porteføljejustering januar 2026.",
            )
        }
    }
}
