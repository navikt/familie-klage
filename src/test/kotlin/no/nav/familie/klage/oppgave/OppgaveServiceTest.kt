package no.nav.familie.klage.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppgaveServiceTest {

    val behandleSakOppgaveRepository = mockk<BehandleSakOppgaveRepository>()
    val oppgaveClient = mockk<OppgaveClient>()
    val oppgaveService = OppgaveService(behandleSakOppgaveRepository, oppgaveClient)

    val behandlingId = UUID.randomUUID()
    val oppgaveId = 1L

    @Test
    internal fun `skal oppdatere oppgave med behandlingstemaet for klage-tilbakekreving`() {
        val oppgaveSlot = slot<Oppgave>()
        val eksisterendeOppgave = BehandleSakOppgave(
            behandlingId = behandlingId,
            oppgaveId = oppgaveId,
        )

        every { behandleSakOppgaveRepository.findByBehandlingId(behandlingId) } returns eksisterendeOppgave

        every { oppgaveClient.oppdaterOppgave(capture(oppgaveSlot)) } returns oppgaveId
        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)

        assertThat(oppgaveSlot.captured.id).isEqualTo(oppgaveId)
        assertThat(oppgaveSlot.captured.behandlingstema).isEqualTo(Behandlingstema.Tilbakebetaling.value)

        // Sjekker at ingen andre felter blir satt
        assertThat(oppgaveSlot.captured.aktivDato).isNull()
        assertThat(oppgaveSlot.captured.behandlesAvApplikasjon).isNull()
        assertThat(oppgaveSlot.captured.behandlingstype).isNull()
        assertThat(oppgaveSlot.captured.beskrivelse).isNull()
        assertThat(oppgaveSlot.captured.bnr).isNull()
        assertThat(oppgaveSlot.captured.endretAv).isNull()
        assertThat(oppgaveSlot.captured.endretAvEnhetsnr).isNull()
        assertThat(oppgaveSlot.captured.endretTidspunkt).isNull()
        assertThat(oppgaveSlot.captured.ferdigstiltTidspunkt).isNull()
        assertThat(oppgaveSlot.captured.identer).isNull()
        assertThat(oppgaveSlot.captured.journalpostId).isNull()
        assertThat(oppgaveSlot.captured.journalpostkilde).isNull()
        assertThat(oppgaveSlot.captured.mappeId).isNull()
        assertThat(oppgaveSlot.captured.oppgavetype).isNull()
        assertThat(oppgaveSlot.captured.opprettetAv).isNull()
        assertThat(oppgaveSlot.captured.opprettetAvEnhetsnr).isNull()
        assertThat(oppgaveSlot.captured.opprettetTidspunkt).isNull()
        assertThat(oppgaveSlot.captured.orgnr).isNull()
        assertThat(oppgaveSlot.captured.prioritet).isNull()
        assertThat(oppgaveSlot.captured.saksreferanse).isNull()
        assertThat(oppgaveSlot.captured.samhandlernr).isNull()
        assertThat(oppgaveSlot.captured.status).isNull()
        assertThat(oppgaveSlot.captured.tema).isNull()
        assertThat(oppgaveSlot.captured.temagruppe).isNull()
        assertThat(oppgaveSlot.captured.tildeltEnhetsnr).isNull()
        assertThat(oppgaveSlot.captured.tilordnetRessurs).isNull()
        assertThat(oppgaveSlot.captured.versjon).isNull()
    }
}
