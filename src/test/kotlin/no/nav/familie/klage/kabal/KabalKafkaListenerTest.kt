package no.nav.familie.klage.kabal

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KabalKafkaListenerTest {
    private lateinit var listener: KabalKafkaListener

    private val behandlingEventService = mockk<BehandlingEventService>(relaxed = true)

    @BeforeEach
    internal fun setUp() {
        listener = KabalKafkaListener(behandlingEventService)
    }

    @Test
    internal fun `skal kalle på oppgaveService for hendelse med EF som kilde`() {
        listener.listen(lagBehandlingEvent("EF"))

        verify(exactly = 1) { behandlingEventService.handleEvent(any()) }
    }

    @Test
    internal fun `skal ikke kalle på oppgaveService for hendelse med annen kilde enn EF`() {
        listener.listen(lagBehandlingEvent("AO01"))

        verify(exactly = 0) { behandlingEventService.handleEvent(any()) }
    }

    private fun lagBehandlingEvent(kilde: String): String {
        val behandlingEvent =
            BehandlingEvent(
                UUID.randomUUID(),
                "kildeReferanse",
                kilde,
                "kabalReferanse",
                BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                BehandlingDetaljer(
                    KlagebehandlingAvsluttetDetaljer(
                        LocalDateTime.now().minusDays(1),
                        KlageinstansUtfall.MEDHOLD,
                        listOf("journalpostReferanse1", "journalpostReferanse2"),
                    ),
                ),
            )
        return objectMapper.writeValueAsString(behandlingEvent)
    }
}
