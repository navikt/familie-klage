package no.nav.familie.klage.kabal.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.kabal.AnkebehandlingOpprettetDetaljer
import no.nav.familie.klage.kabal.BehandlingDetaljer
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.KlagebehandlingAvsluttetDetaljer
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingEventServiceTest {

    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val fagsakRepository = mockk<FagsakRepository>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val stegService = mockk<StegService>(relaxed = true)
    private val klageresultatRepository = mockk<KlageresultatRepository>(relaxed = true)

    val behandlingEventService = BehandlingEventService(
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        stegService = stegService,
        taskService = taskService,
        klageresultatRepository = klageresultatRepository
    )

    val behandlingMedStatusVenter = DomainUtil.behandling(status = BehandlingStatus.VENTER)

    @BeforeEach
    fun setUp() {
        every { taskService.save(any()) } answers { firstArg() }
        every { behandlingRepository.findByEksternBehandlingId(any()) } returns behandlingMedStatusVenter
        every { klageresultatRepository.insert(any()) } answers { firstArg() }
        every { klageresultatRepository.existsById(any()) } returns false
    }

    @Test
    fun `Skal lage oppgave og ferdigstille behandling for klage som er ikke er ferdigstilt`() {
        val behandlingEvent = lagBehandlingEvent()

        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 1) {
            stegService.oppdaterSteg(
                behandlingMedStatusVenter.id,
                any(),
                StegType.BEHANDLING_FERDIGSTILT
            )
        }
    }

    @Test
    fun `Skal ikke ferdigstille behandling, bare lage oppgave, n??r event er av type anke`() {
        val behandlingEvent = lagBehandlingEvent(
            behandlingEventType = BehandlingEventType.ANKEBEHANDLING_OPPRETTET,
            behandlingDetaljer = BehandlingDetaljer(
                ankebehandlingOpprettet = AnkebehandlingOpprettetDetaljer(LocalDateTime.now())
            )
        )

        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(any(), any(), any()) }
    }

    @Test
    fun `Skal ikke behandle klage som er er ferdigstilt`() {
        val behandlingEvent = lagBehandlingEvent()
        val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandlingRepository.findByEksternBehandlingId(any()) } returns behandling
        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(behandling.id, any(), StegType.BEHANDLING_FERDIGSTILT) }
    }

    @Test
    internal fun `Skal ikke behandle event hvis det allerede er behandlet`() {
        every { klageresultatRepository.existsById(any()) } returns true

        behandlingEventService.handleEvent(lagBehandlingEvent())

        verify(exactly = 0) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 0) { klageresultatRepository.insert(any()) }
    }

    @Test
    internal fun `Skal lagre event hvis det ikke allerede er behandlet`() {
        behandlingEventService.handleEvent(lagBehandlingEvent())

        verify(exactly = 1) { behandlingRepository.findByEksternBehandlingId(any()) }
        verify(exactly = 1) { klageresultatRepository.insert(any()) }
    }

    private fun lagBehandlingEvent(
        behandlingEventType: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
        behandlingDetaljer: BehandlingDetaljer? = null
    ): BehandlingEvent {
        return BehandlingEvent(
            eventId = UUID.randomUUID(),
            kildeReferanse = UUID.randomUUID().toString(),
            kilde = "EF",
            kabalReferanse = "kabalReferanse",
            type = behandlingEventType,
            detaljer = behandlingDetaljer ?: BehandlingDetaljer(
                KlagebehandlingAvsluttetDetaljer(
                    LocalDateTime.now().minusDays(1),
                    KlageinstansUtfall.MEDHOLD,
                    listOf("journalpostReferanse1", "journalpostReferanse2")
                )
            )
        )
    }
}
