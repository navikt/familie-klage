package no.nav.familie.klage.kabal.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakPersonRepository
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.integrasjoner.OppgaveClient
import no.nav.familie.klage.kabal.BehandlingDetaljer
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.klage.kabal.ExternalUtfall
import no.nav.familie.klage.kabal.KlagebehandlingAvsluttetDetaljer
import no.nav.familie.klage.testutil.DomainUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingEventServiceTest {

    private val oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val fagsakRepository = mockk<FagsakRepository>(relaxed = true)
    private val fagsakPersonRepository = mockk<FagsakPersonRepository>(relaxed = true)
    private val stegService = mockk<StegService>(relaxed = true)

    val behandlingEventService = BehandlingEventService(
        oppgaveClient = oppgaveClient,
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        personRepository = fagsakPersonRepository,
        stegService = stegService
    )

    val behandlingMedStatusVenter = DomainUtil.behandling(status = BehandlingStatus.VENTER)

    @BeforeEach
    fun setUp() {
        every { behandlingRepository.findByEksternBehandlingIdAndFagsystem(any(), any()) } returns behandlingMedStatusVenter
    }

    @Test
    fun `Skal lage oppgave og ferdigstille behandling for klage som er ikke er ferdigstilt`() {
        val behandlingEvent = lagBehandlingEvent()

        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 1) { oppgaveClient.opprettOppgave(any()) }
        verify(exactly = 1) { stegService.oppdaterSteg(behandlingMedStatusVenter.id, StegType.BEHANDLING_FERDIGSTILT) }
    }

    @Test
    fun `Skal ikke ferdigstille behandling, bare lage oppgave, når event er av type anke`() {
        val behandlingEvent = lagBehandlingEvent(behandlingEventType = BehandlingEventType.ANKEBEHANDLING_OPPRETTET)

        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 1) { oppgaveClient.opprettOppgave(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(any(), any()) }
    }

    @Test
    fun `Skal ikke behandle klage som er er ferdigstilt`() {
        val behandlingEvent = lagBehandlingEvent()
        val behandling = DomainUtil.behandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandlingRepository.findByEksternBehandlingIdAndFagsystem(any(), any()) } returns behandling
        behandlingEventService.handleEvent(behandlingEvent)

        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
        verify(exactly = 0) { stegService.oppdaterSteg(behandling.id, StegType.BEHANDLING_FERDIGSTILT) }
    }

    private fun lagBehandlingEvent(behandlingEventType: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET): BehandlingEvent {
        val behandlingEvent = BehandlingEvent(
            eventId = UUID.randomUUID(),
            kildeReferanse = "kildeReferanse",
            kilde = "EF",
            kabalReferanse = "kabalReferanse",
            type = behandlingEventType,
            detaljer = BehandlingDetaljer(
                KlagebehandlingAvsluttetDetaljer(
                    LocalDateTime.now().minusDays(1),
                    ExternalUtfall.MEDHOLD,
                    listOf("journalpostReferanse1", "journalpostReferanse2")
                )
            )
        )
        return behandlingEvent
    }
}
